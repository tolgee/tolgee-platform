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
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.oauth2.OAuth2ClientDocumentCheckRepository
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * What a document check learns about a client that identifies itself with a metadata document, written to
 * `oauth2_client_document_check` and, for a drifted document, to the grant rows. [CimdDocumentCheck] calls it from
 * the refresh path; the grant flow only reads the marks it leaves. `docs/oauth/README.md` explains each rule and
 * what it stops.
 *
 * Every write runs in its own transaction. The check happens inside a token request, and what it learned about the
 * publisher must stay written whatever becomes of that request.
 */
@Service
class CimdClientLifecycleService(
  private val oauth2GrantRepository: OAuth2GrantRepository,
  private val documentCheckRepository: OAuth2ClientDocumentCheckRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val properties: OAuth2ServerProperties,
) : Logging {
  /**
   * Takes this client's next attempt if the last one is older than `cimd-check-interval-minutes`. Returns the
   * attempt time, or null when another caller got there first or the client is not due.
   *
   * The first-ever attempt inserts the row, and two callers racing for it make the second insert fail on the unique
   * constraint. The caller treats that failure as "not claimed".
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun claimCheck(clientId: String): Date? {
    val now = currentDateProvider.date
    if (documentCheckRepository.claimAttempt(clientId, now, dueBefore(now)) > 0) return now
    if (documentCheckRepository.existsByClientId(clientId)) return null
    documentCheckRepository.save(
      OAuth2ClientDocumentCheck().also {
        it.clientId = clientId
        it.checkedAt = now
      },
    )
    return now
  }

  /** Gives a claim back when the fetch was refused before it reached the publisher: that was no attempt. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun releaseCheck(
    clientId: String,
    claimedAt: Date,
  ) {
    if (documentCheckRepository.releaseAttempt(clientId, claimedAt) > 0) return
    documentCheckRepository.deleteFirstAttempt(clientId, claimedAt)
  }

  /** Writes what the publisher answered to the client's check row, and revokes the grants its document drifted from. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun recordCheckResult(
    clientId: String,
    resolution: CimdResolution,
  ) {
    if (resolution is CimdResolution.Resolved) {
      revokeDriftedFromDocument(clientId, resolution.client.client.metadataHash)
      clearClientWithdrawn(clientId)
      documentCheckRepository.findByClientId(clientId)?.let { it.failingSince = null }
      return
    }
    markFailing(clientId)
    if (resolution == CimdResolution.Withdrawn) {
      recordClientWithdrawn(clientId)
      return
    }
    logger.info("CIMD check could not read the document of {}: {}", clientId, resolution)
  }

  /** What this server knows about the client's document. Null when nothing has ever tried to read it. */
  fun documentCheckOf(clientId: String): OAuth2ClientDocumentCheck? = documentCheckRepository.findByClientId(clientId)

  fun isGrantWithdrawn(grant: OAuth2Grant): Boolean =
    documentCheckRepository.findByClientId(grant.clientId)?.covers(grant) == true

  @Transactional
  fun deleteCheckRowsWithoutGrants(): Int = documentCheckRepository.deleteWithoutGrants()

  /** A publisher's refusal, made durable on the client's row so every instance reads it. */
  @Transactional
  fun recordClientWithdrawn(clientId: String) {
    val now = currentDateProvider.date
    val row =
      documentCheckRepository.findByClientId(clientId)
        ?: OAuth2ClientDocumentCheck().also {
          it.clientId = clientId
          it.checkedAt = now
        }
    if (row.withdrawnAt != null) return
    row.withdrawnAt = now
    documentCheckRepository.save(row)
    logger.warn("Marked client {} as withdrawn: its metadata document is gone", clientId)
  }

  private fun markFailing(clientId: String) {
    val row = documentCheckRepository.findByClientId(clientId) ?: return
    if (row.failingSince != null) return
    row.failingSince = currentDateProvider.date
  }

  /** The document no longer matches what its users agreed to, so those grants end. */
  private fun revokeDriftedFromDocument(
    clientId: String,
    currentHash: String?,
  ) {
    if (currentHash == null) return
    val revoked =
      oauth2GrantRepository.deleteDriftedFromDocument(
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
  }

  /**
   * The document resolves again. A mark younger than the grace window was a mis-deploy and is lifted; so is one no
   * check has happened since, however old. An older one that a check has already seen is the publisher retiring the
   * client and stays.
   */
  private fun clearClientWithdrawn(clientId: String) {
    val row = documentCheckRepository.findByClientId(clientId) ?: return
    val mark = row.withdrawnAt ?: return
    // The claim for this check already moved the row on, so `previousCheckedAt` is the attempt before this one. A
    // mark newer than it was written by that very attempt and has not survived a read yet; a mark older than it has.
    val previousAttempt = row.previousCheckedAt ?: NEVER_CHECKED
    if (!mark.after(graceStart()) && !mark.after(previousAttempt)) return
    row.withdrawnAt = null
    logger.info("Lifted the withdrawal mark on client {}: its document answers again", clientId)
  }

  private fun dueBefore(now: Date): Date =
    Date(now.time - TimeUnit.MINUTES.toMillis(properties.cimdCheckIntervalMinutes))

  private fun graceStart(): Date =
    Date(currentDateProvider.date.time - TimeUnit.MINUTES.toMillis(properties.cimdWithdrawalGraceMinutes))

  companion object {
    private val NEVER_CHECKED = Date(0)
  }
}
