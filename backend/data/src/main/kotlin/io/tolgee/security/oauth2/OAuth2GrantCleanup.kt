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

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class OAuth2GrantCleanup(
  private val authorizationService: OAuth2AuthorizationService,
  private val properties: OAuth2ServerProperties,
  private val currentDateProvider: CurrentDateProvider,
  private val lockingProvider: LockingProvider,
) : Logging {
  @Scheduled(
    cron =
      "\${tolgee.oauth2.grant-cleanup-cron:${OAuth2ServerProperties.DEFAULT_GRANT_CLEANUP_CRON}}",
  )
  fun cleanUpExpiredGrants() {
    lockingProvider.withLockingIfFree(CLEANUP_LOCK_NAME, CLEANUP_LOCK_LEASE_TIME) {
      purgeExpiredGrants()
    }
  }

  private fun purgeExpiredGrants() {
    val cutoff = currentDateProvider.date.toInstant().minus(Duration.ofDays(properties.grantRetentionDays))
    val deleted = authorizationService.deleteExpiredBefore(cutoff) + authorizationService.deleteExpiredPendingConsents()
    if (deleted > 0) {
      logger.info("OAuth2 grant cleanup removed {} expired grant(s)", deleted)
    }
  }

  companion object {
    private const val CLEANUP_LOCK_NAME = "oauth2_grant_cleanup_lock"
    private val CLEANUP_LOCK_LEASE_TIME = Duration.ofMinutes(10)
  }
}
