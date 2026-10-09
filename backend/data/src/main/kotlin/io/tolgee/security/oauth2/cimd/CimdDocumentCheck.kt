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

import io.tolgee.Metrics
import io.tolgee.security.oauth2.OAuth2ClientRegistry
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

/**
 * Re-reads the metadata document of a client that holds a grant, from the refresh path, once per
 * `cimd-check-interval-minutes` per client. What it finds goes to the grant rows: a document that is gone retires
 * the client, one that answers keeps its grants alive.
 *
 * The caller must hold a validated refresh token of a live grant of this client. That is what makes the fetch
 * something only a consenting user can start, and the per-client interval is what bounds how often. `docs/oauth/README.md`
 * says why it is read here and not on a schedule.
 */
@Component
class CimdDocumentCheck(
  private val lifecycle: CimdClientLifecycleService,
  private val oauth2ClientRegistry: OAuth2ClientRegistry,
  private val budget: CimdFetchBudget,
  private val metrics: Metrics,
) : Logging {
  enum class Outcome {
    /** Not a document-backed client, or read within the interval already. Nothing was fetched. */
    NOT_DUE,

    /** This server had no room for the fetch. Nothing was recorded, so the client is due again at once. */
    NOT_ATTEMPTED,

    CHECKED,
  }

  fun checkIfDue(clientId: String): Outcome {
    if (!oauth2ClientRegistry.servesCimdClient(clientId)) return Outcome.NOT_DUE
    val outcome = budget.withBudget(clientId) { claimAndCheck(clientId) }
    if (outcome != null) return outcome
    metrics.oauth2CimdCapacityRefusalsCounter.increment()
    return Outcome.NOT_ATTEMPTED
  }

  private fun claimAndCheck(clientId: String): Outcome {
    val claimedAt = claim(clientId) ?: return Outcome.NOT_DUE
    val resolution =
      try {
        oauth2ClientRegistry.resolveForCheck(clientId)
      } catch (e: Exception) {
        lifecycle.releaseCheck(clientId, claimedAt)
        throw e
      }
    if (resolution == null) {
      lifecycle.releaseCheck(clientId, claimedAt)
      return Outcome.NOT_ATTEMPTED
    }
    lifecycle.recordCheckResult(clientId, resolution)
    return Outcome.CHECKED
  }

  private fun claim(clientId: String) =
    try {
      lifecycle.claimCheck(clientId)
    } catch (_: DataIntegrityViolationException) {
      logger.debug("CIMD check for {} lost the race for the first attempt", clientId)
      null
    }
}
