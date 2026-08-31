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

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant
import java.util.Date

class OAuth2GrantCleanupTest : AbstractSpringTest() {
  @Autowired
  private lateinit var repository: OAuth2GrantRepository

  @Autowired
  private lateinit var authorizationService: OAuth2AuthorizationService

  private lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `deletes only authorizations whose credentials all expired before the cutoff`() {
    val now = Instant.now()
    val old = now.minus(Duration.ofDays(10))
    val recent = now.minus(Duration.ofDays(2))
    val future = now.plus(Duration.ofDays(20))

    val expiredLongAgo = insert(refreshExpiresAt = old)
    val fresh = insert(refreshExpiresAt = future)
    val recentlyExpired = insert(refreshExpiresAt = recent)
    val liveRefresh = insert(refreshExpiresAt = future, accessExpiresAt = old, codeExpiresAt = old)
    val accessOnly = insert(refreshExpiresAt = null, accessExpiresAt = old, codeExpiresAt = null)

    val deleted = authorizationService.deleteExpiredBefore(now.minus(Duration.ofDays(7)))

    deleted.assert.isEqualTo(2)
    repository.existsById(expiredLongAgo).assert.isFalse()
    repository.existsById(accessOnly).assert.isFalse()
    repository.existsById(fresh).assert.isTrue()
    repository.existsById(recentlyExpired).assert.isTrue()
    repository.existsById(liveRefresh).assert.isTrue()
  }

  @Test
  fun `an expired pending consent is reaped on its own deadline, not the retention window`() {
    val now = Instant.now()
    // A consent nobody completed holds no code and no tokens, so the 7-day window that protects a spent code's
    // replay evidence has nothing to protect here.
    val abandonedConsent = insert(refreshExpiresAt = null, consentExpiresAt = now.minus(Duration.ofMinutes(20)))
    val liveConsent = insert(refreshExpiresAt = null, consentExpiresAt = now.plus(Duration.ofMinutes(10)))
    val spentGrant = insert(refreshExpiresAt = now.plus(Duration.ofDays(20)))

    val deleted = authorizationService.deleteExpiredPendingConsents()

    deleted.assert.isEqualTo(1)
    repository.existsById(abandonedConsent).assert.isFalse()
    repository.existsById(liveConsent).assert.isTrue()
    repository.existsById(spentGrant).assert.isTrue()
  }

  private fun insert(
    refreshExpiresAt: Instant?,
    accessExpiresAt: Instant? = null,
    codeExpiresAt: Instant? = null,
    consentExpiresAt: Instant? = null,
  ): Long {
    val grant =
      OAuth2Grant().apply {
        userAccount = testData.user
        clientId = "cleanup-test-client"
        redirectUri = "https://example.org/callback"
        codeChallenge = "challenge"
        requestedScopes = "translations.view"
        this.refreshTokenExpiresAt = refreshExpiresAt?.let { Date.from(it) }
        this.accessTokenExpiresAt = accessExpiresAt?.let { Date.from(it) }
        this.codeExpiresAt = codeExpiresAt?.let { Date.from(it) }
        this.consentExpiresAt = consentExpiresAt?.let { Date.from(it) }
      }
    repository.save(grant)
    return grant.id
  }
}
