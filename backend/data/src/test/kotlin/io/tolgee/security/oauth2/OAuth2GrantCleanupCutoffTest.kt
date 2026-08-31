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
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Duration
import java.time.Instant
import java.util.Date

/**
 * The cutoff computation is the only logic in the scheduled cleanup: a sign error would push it into the future and
 * delete authorizations whose refresh tokens are still live, logging active users out.
 */
class OAuth2GrantCleanupCutoffTest {
  @Test
  fun `deletes with a cutoff of exactly now minus the retention window`() {
    val now = Instant.parse("2026-08-07T00:00:00Z")
    val authorizationService = mock<OAuth2AuthorizationService>()
    val dateProvider = mock<CurrentDateProvider> { on { date } doReturn Date.from(now) }
    val properties = OAuth2ServerProperties().apply { grantRetentionDays = 7 }

    OAuth2GrantCleanup(authorizationService, properties, dateProvider).cleanUpExpiredGrants()

    val captor = argumentCaptor<Instant>()
    verify(authorizationService).deleteExpiredBefore(captor.capture())
    captor.firstValue.assert.isEqualTo(now.minus(Duration.ofDays(7)))
  }
}
