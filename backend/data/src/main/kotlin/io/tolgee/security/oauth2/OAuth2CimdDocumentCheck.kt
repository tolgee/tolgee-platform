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

package io.tolgee.security.oauth2

import io.tolgee.Metrics
import io.tolgee.component.LockingProvider
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.security.oauth2.cimd.CimdClientLifecycleService
import io.tolgee.security.oauth2.cimd.CimdResolution
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * Re-reads the metadata documents of the clients that hold grants, and writes what it finds to the grant rows: a
 * document that is gone retires the client, one that answers keeps its grants alive.
 *
 * This is the **only** place a document is read for a client that already has a grant. `docs/oauth/README.md`
 * explains why, and how the work list is ordered and bounded.
 */
@Component
class OAuth2CimdDocumentCheck(
  private val lifecycle: CimdClientLifecycleService,
  private val clientRegistry: OAuth2ClientRegistry,
  private val properties: OAuth2ServerProperties,
  private val lockingProvider: LockingProvider,
  private val metrics: Metrics,
) : Logging {
  private val backlog = AtomicLong(0)

  @Scheduled(
    cron = "\${tolgee.oauth2.cimd-check-cron:${OAuth2ServerProperties.DEFAULT_CIMD_CHECK_CRON}}",
  )
  fun checkClientDocuments() {
    if (!properties.cimdEnabled) return
    lockingProvider.withLockingIfFree(CHECK_LOCK_NAME, CHECK_LOCK_LEASE_TIME) {
      checkBatch()
    }
  }

  @PostConstruct
  fun registerBacklogGauge() {
    metrics.registerCimdCheckBacklog { backlog.get() }
  }

  /** One round of the schedule. Returns for how many clients the round wrote a durable result. */
  fun checkBatch(): Int {
    val due =
      try {
        lifecycle.clientIdsDueForCheck(properties.cimdCheckBatchSize)
      } catch (e: Exception) {
        logger.error("CIMD check could not build its work list", e)
        return 0
      }
    var written = 0
    due.forEach { clientId -> if (checkOne(clientId)) written++ }
    recordBacklog()
    return written
  }

  private fun recordBacklog() {
    try {
      measureBacklog()
    } catch (e: Exception) {
      logger.error("CIMD check could not measure its backlog", e)
    }
  }

  private fun measureBacklog() {
    val waiting = lifecycle.clientsDueForCheckCount()
    backlog.set(waiting)
    if (waiting <= properties.cimdCheckBatchSize.toLong() * BACKLOG_ROUNDS_BEFORE_WARNING) return
    logger.warn(
      "CIMD check backlog is {} clients at {} per round: a client is re-read too rarely to keep its grants inside " +
        "tolgee.oauth2.cimd-verification-max-age-days, and a withdrawal takes that long to be noticed",
      waiting,
      properties.cimdCheckBatchSize,
    )
  }

  private fun checkOne(clientId: String): Boolean {
    try {
      val resolution = clientRegistry.resolveForCheck(clientId) ?: return false
      return recordResolution(clientId, resolution)
    } catch (e: Exception) {
      logger.error("CIMD check failed for {}", clientId, e)
      return false
    } finally {
      runCatching { lifecycle.recordCheckAttempt(clientId) }
        .onFailure { logger.error("Could not record the CIMD check attempt for {}", clientId, it) }
    }
  }

  /** Writes what the resolution means to the grant rows. Returns whether anything durable was written. */
  private fun recordResolution(
    clientId: String,
    resolution: CimdResolution,
  ): Boolean {
    if (resolution is CimdResolution.Resolved) {
      lifecycle.revokeDriftedFromDocument(clientId, resolution.client.client.metadataHash)
      lifecycle.recordDocumentRead(clientId)
      lifecycle.clearClientWithdrawn(clientId)
      return true
    }
    if (resolution == CimdResolution.Withdrawn) {
      lifecycle.recordClientWithdrawn(clientId)
      return true
    }
    logger.info("CIMD check could not read the document of {}: {}", clientId, resolution)
    return false
  }

  companion object {
    private const val BACKLOG_ROUNDS_BEFORE_WARNING = 10

    private const val CHECK_LOCK_NAME = "oauth2_cimd_document_check_lock"
    private val CHECK_LOCK_LEASE_TIME = Duration.ofMinutes(15)
  }
}
