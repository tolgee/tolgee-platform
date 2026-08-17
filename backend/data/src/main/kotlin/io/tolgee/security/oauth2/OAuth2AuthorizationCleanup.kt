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
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class OAuth2AuthorizationCleanup(
  private val queryService: OAuth2AuthorizationQueryService,
  private val properties: OAuth2ServerProperties,
  private val currentDateProvider: CurrentDateProvider,
) : Logging {
  @Scheduled(cron = "\${tolgee.oauth2.authorization-cleanup-cron:0 0 3 * * *}")
  fun cleanUpExpiredAuthorizations() {
    val cutoff = currentDateProvider.date.toInstant().minus(Duration.ofDays(properties.authorizationRetentionDays))
    val deleted = queryService.deleteExpiredBefore(cutoff)
    if (deleted > 0) {
      logger.info("OAuth2 authorization cleanup removed {} expired authorization(s)", deleted)
    }
  }
}
