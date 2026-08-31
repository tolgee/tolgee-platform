package io.tolgee.security.oauth2

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.OAuth2GrantCleanupTestData
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant

class OAuth2GrantCleanupTest : AbstractSpringTest() {
  @Autowired
  private lateinit var repository: OAuth2GrantRepository

  @Autowired
  private lateinit var authorizationService: OAuth2AuthorizationService

  private lateinit var testData: OAuth2GrantCleanupTestData

  @BeforeEach
  fun setup() {
    testData = OAuth2GrantCleanupTestData()
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

    val expiredLongAgo = testData.addGrant(refreshExpiresAt = old)
    val fresh = testData.addGrant(refreshExpiresAt = future)
    val recentlyExpired = testData.addGrant(refreshExpiresAt = recent)
    val liveRefresh = testData.addGrant(refreshExpiresAt = future, accessExpiresAt = old, codeExpiresAt = old)
    val accessOnly = testData.addGrant(accessExpiresAt = old)
    // Approved but never exchanged: holds a code and nothing else, so this query is its only reaper —
    // deleteExpiredPendingConsents skips it on the codeHash.
    val codeOnly = testData.addGrant(codeExpiresAt = old, codeHash = "abandoned-code")
    testDataService.saveTestData(testData.root)

    val deleted = authorizationService.deleteExpiredBefore(now.minus(Duration.ofDays(7)))

    deleted.assert.isEqualTo(3)
    repository.existsById(expiredLongAgo.id).assert.isFalse()
    repository.existsById(accessOnly.id).assert.isFalse()
    repository.existsById(codeOnly.id).assert.isFalse()
    repository.existsById(fresh.id).assert.isTrue()
    repository.existsById(recentlyExpired.id).assert.isTrue()
    repository.existsById(liveRefresh.id).assert.isTrue()
  }

  @Test
  fun `an expired pending consent is reaped on its own deadline, not the retention window`() {
    val now = Instant.now()
    // A consent nobody completed holds no code and no tokens, so the 7-day window that protects a spent code's
    // replay evidence has nothing to protect here.
    val abandonedConsent = testData.addGrant(consentExpiresAt = now.minus(Duration.ofMinutes(20)))
    val liveConsent = testData.addGrant(consentExpiresAt = now.plus(Duration.ofMinutes(10)))
    val spentGrant = testData.addGrant(refreshExpiresAt = now.plus(Duration.ofDays(20)))
    testDataService.saveTestData(testData.root)

    val deleted = authorizationService.deleteExpiredPendingConsents()

    deleted.assert.isEqualTo(1)
    repository.existsById(abandonedConsent.id).assert.isFalse()
    repository.existsById(liveConsent.id).assert.isTrue()
    repository.existsById(spentGrant.id).assert.isTrue()
  }
}
