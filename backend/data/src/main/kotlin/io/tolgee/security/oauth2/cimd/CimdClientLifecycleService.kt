/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.oauth2.cimd

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.model.oauth2.OAuth2ClientDocumentCheck
import io.tolgee.repository.oauth2.OAuth2ClientDocumentCheckRepository
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * What the background check learns about a client that identifies itself with a metadata document, written to the
 * grant rows and to `oauth2_client_document_check`. The scheduled check and the cleanup job call it; the grant flow
 * only reads the marks it leaves. `docs/oauth/README.md` explains each rule and what it stops.
 */
@Service
class CimdClientLifecycleService(
  private val grantRepository: OAuth2GrantRepository,
  private val documentCheckRepository: OAuth2ClientDocumentCheckRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val properties: OAuth2ServerProperties,
) : Logging {
  @Transactional
  fun recordDocumentRead(clientId: String) {
    val now = currentDateProvider.date
    val refreshBefore = Date(now.time - TimeUnit.DAYS.toMillis(properties.cimdVerificationMaxAgeDays) / 2)
    grantRepository.markClientDocumentRead(clientId, now, refreshBefore)
  }

  /** The document no longer matches what its users agreed to, so those grants end. */
  @Transactional
  fun revokeDriftedFromDocument(
    clientId: String,
    currentHash: String?,
  ): Int {
    if (currentHash == null) return 0
    val revoked =
      grantRepository.deleteDriftedFromDocument(
        clientId,
        currentHash,
        CimdMetadataFetcher.HASH_SCHEME_PREFIX + "%",
      )
    if (revoked > 0) {
      logger.warn(
        "Revoked {} OAuth2 grant(s) of client {}: its metadata document no longer matches the consented terms",
        revoked,
        clientId,
      )
    }
    return revoked
  }

  fun clientIdsDueForCheck(limit: Int): List<String> =
    grantRepository.findCimdClientIdsDueForCheck(
      currentDateProvider.date,
      dueBefore(),
      graceStart(),
      NEVER_CHECKED,
      PageRequest.of(0, limit),
    )

  fun clientsDueForCheckCount(): Long =
    grantRepository.countCimdClientIdsDueForCheck(currentDateProvider.date, dueBefore(), graceStart(), NEVER_CHECKED)

  @Transactional
  fun recordCheckAttempt(clientId: String) {
    val existing = documentCheckRepository.findByClientId(clientId)
    val row = existing ?: OAuth2ClientDocumentCheck().also { it.clientId = clientId }
    row.previousCheckedAt = existing?.checkedAt
    row.checkedAt = currentDateProvider.date
    documentCheckRepository.save(row)
  }

  /** When this server last tried to read the client's document, whatever came of it. */
  fun lastCheckAttempt(clientId: String): Date? = documentCheckRepository.findByClientId(clientId)?.checkedAt

  @Transactional
  fun deleteCheckRowsWithoutGrants(): Int = documentCheckRepository.deleteWithoutGrants()

  /** A publisher's refusal, made durable on the grant rows so every instance reads it. */
  @Transactional
  fun recordClientWithdrawn(clientId: String) {
    val marked = grantRepository.markClientWithdrawn(clientId, currentDateProvider.date)
    if (marked > 0) {
      logger.warn(
        "Marked {} OAuth2 grant(s) of client {} as withdrawn: its metadata document refuses",
        marked,
        clientId,
      )
    }
  }

  /**
   * The document resolves again. A mark younger than the grace window was a mis-deploy and is lifted; an older one
   * is the publisher retiring the client and stays.
   */
  @Transactional
  fun clearClientWithdrawn(clientId: String) {
    // This round's attempt is recorded after the work, so the row still holds the previous round's and
    // `previousCheckedAt` reaches two attempts back. Comparing against the latest one instead would make every
    // mark look "already read" on the very next round.
    val previousAttempt = documentCheckRepository.findByClientId(clientId)?.previousCheckedAt ?: NEVER_CHECKED
    val cleared = grantRepository.clearRecentClientWithdrawn(clientId, graceStart(), previousAttempt)
    if (cleared > 0) {
      logger.info(
        "Lifted the withdrawal mark on {} OAuth2 grant(s) of client {}: its document answers again",
        cleared,
        clientId,
      )
    }
  }

  private fun dueBefore(): Date =
    Date(currentDateProvider.date.time - TimeUnit.MINUTES.toMillis(properties.cimdCheckIntervalMinutes))

  private fun graceStart(): Date =
    Date(currentDateProvider.date.time - TimeUnit.MINUTES.toMillis(properties.cimdWithdrawalGraceMinutes))

  companion object {
    private val NEVER_CHECKED = Date(0)
  }
}
