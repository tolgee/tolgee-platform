package io.tolgee.security.oauth2

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.oauth2.OAuth2Authorization
import io.tolgee.repository.oauth2.OAuth2AuthorizationRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant
import java.util.Date

class OAuth2AuthorizationCleanupTest : AbstractSpringTest() {
  @Autowired
  private lateinit var repository: OAuth2AuthorizationRepository

  @Autowired
  private lateinit var queryService: OAuth2AuthorizationService

  private lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    queryService.revokeAllForUser(testData.user.id)
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

    val deleted = queryService.deleteExpiredBefore(now.minus(Duration.ofDays(7)))

    deleted.assert.isEqualTo(2)
    repository.existsById(expiredLongAgo).assert.isFalse()
    repository.existsById(accessOnly).assert.isFalse()
    repository.existsById(fresh).assert.isTrue()
    repository.existsById(recentlyExpired).assert.isTrue()
    repository.existsById(liveRefresh).assert.isTrue()
  }

  @Test
  fun `deletes abandoned pre-consent rows (all expiries NULL) older than the cutoff but keeps in-flight ones`() {
    val now = Instant.now()
    val abandoned = insert(refreshExpiresAt = null, createdAt = now.minus(Duration.ofDays(10)))
    val inFlight = insert(refreshExpiresAt = null, createdAt = now)

    queryService.deleteExpiredBefore(now.minus(Duration.ofDays(7)))

    repository.existsById(abandoned).assert.isFalse()
    repository.existsById(inFlight).assert.isTrue()
  }

  private fun insert(
    refreshExpiresAt: Instant?,
    accessExpiresAt: Instant? = null,
    codeExpiresAt: Instant? = null,
    createdAt: Instant? = null,
  ): Long {
    // createdAt is stamped by auditing from the current-date provider, so an old row is planted by forcing the clock.
    currentDateProvider.forcedDate = createdAt?.let { Date.from(it) }
    val authorization =
      OAuth2Authorization().apply {
        userAccount = testData.user
        clientId = "cleanup-test-client"
        redirectUri = "https://example.org/callback"
        codeChallenge = "challenge"
        requestedScopes = "translations.view"
        this.refreshTokenExpiresAt = refreshExpiresAt?.let { Date.from(it) }
        this.accessTokenExpiresAt = accessExpiresAt?.let { Date.from(it) }
        this.codeExpiresAt = codeExpiresAt?.let { Date.from(it) }
      }
    repository.save(authorization)
    currentDateProvider.forcedDate = null
    return authorization.id
  }
}
