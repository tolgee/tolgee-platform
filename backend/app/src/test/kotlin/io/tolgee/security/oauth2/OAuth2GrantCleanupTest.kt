package io.tolgee.security.oauth2

import io.tolgee.AbstractSpringTest
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.development.testDataBuilder.data.OAuth2GrantCleanupTestData
import io.tolgee.repository.oauth2.OAuth2ClientDocumentCheckRepository
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.repository.oauth2.OAuth2SupersededRefreshTokenRepository
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
  private lateinit var supersededRepository: OAuth2SupersededRefreshTokenRepository

  @Autowired
  private lateinit var documentCheckRepository: OAuth2ClientDocumentCheckRepository

  @Autowired
  private lateinit var properties: OAuth2ServerProperties

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
    // Check rows are keyed by client_id alone, so nothing in the test-data graph owns them.
    listOf(LIVE_CLIENT, ORPHAN_CLIENT).forEach { clientId ->
      documentCheckRepository.findByClientId(clientId)?.let { documentCheckRepository.delete(it) }
    }
  }

  @Test
  fun `only the check rows of clients nobody holds a grant for are deleted`() {
    testData.addGrant(refreshExpiresAt = Instant.now().plus(Duration.ofDays(20)), clientId = LIVE_CLIENT)
    testDataService.saveTestData(testData.root)
    authorizationService.recordCheckAttempt(LIVE_CLIENT)
    authorizationService.recordCheckAttempt(ORPHAN_CLIENT)

    authorizationService.deleteCheckRowsWithoutGrants()

    documentCheckRepository.findByClientId(LIVE_CLIENT).assert.isNotNull()
    documentCheckRepository.findByClientId(ORPHAN_CLIENT).assert.isNull()
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

  @Test
  fun `only the rotations beyond the per-grant depth are pruned, however long ago they happened`() {
    val now = Instant.now()
    val keep = properties.refreshTokenHistoryGenerations
    val liveGrant = testData.addGrant(refreshExpiresAt = now.plus(Duration.ofDays(20)))
    val ancientButShallow =
      testData.addSupersededRefreshToken(liveGrant, "ancient-hash", now.minus(Duration.ofDays(300)))
    val newest =
      (1..keep).map { testData.addSupersededRefreshToken(liveGrant, "hash-$it", now.minusSeconds(it.toLong())) }
    testDataService.saveTestData(testData.root)

    authorizationService.pruneRefreshHistoryBeyondDepth()

    supersededRepository.countByGrantId(liveGrant.id).assert.isEqualTo(keep.toLong())
    supersededRepository.existsById(ancientButShallow.id).assert.isFalse()
    newest.forEach { supersededRepository.existsById(it.id).assert.isTrue() }
    repository.existsById(liveGrant.id).assert.isTrue()
  }

  @Test
  fun `depth is counted per grant, so one busy grant does not consume another grant's history`() {
    val now = Instant.now()
    val keep = properties.refreshTokenHistoryGenerations
    val pastFloor = now.minus(Duration.ofDays(properties.refreshTokenHistoryMinDays + 1))
    val busy = testData.addGrant(refreshExpiresAt = now.plus(Duration.ofDays(20)))
    val quiet = testData.addGrant(refreshExpiresAt = now.plus(Duration.ofDays(20)))
    (1..keep + 5).forEach { testData.addSupersededRefreshToken(busy, "busy-$it", pastFloor.minusSeconds(it.toLong())) }
    val quietOnly = testData.addSupersededRefreshToken(quiet, "quiet-1", now.minus(Duration.ofDays(200)))
    testDataService.saveTestData(testData.root)

    authorizationService.pruneRefreshHistoryBeyondDepth()

    supersededRepository.existsById(quietOnly.id).assert.isTrue()
    supersededRepository.countByGrantId(busy.id).assert.isEqualTo(keep.toLong())
    supersededRepository.countByGrantId(quiet.id).assert.isEqualTo(1L)
  }

  @Test
  fun `a burst of rotations cannot evict a row that is still younger than the floor`() {
    val now = Instant.now()
    val grant = testData.addGrant(refreshExpiresAt = now.plus(Duration.ofDays(20)))
    val stolen = testData.addSupersededRefreshToken(grant, "stolen", now.minus(Duration.ofHours(1)))
    val burst =
      (1..OAuth2AuthorizationService.MAX_HISTORY_ROWS_PER_GRANT + 3)
        .map { testData.addSupersededRefreshToken(grant, "burst-$it", now.minusSeconds(it.toLong())) }
    testDataService.saveTestData(testData.root)

    authorizationService.pruneRefreshHistoryBeyondDepth()

    supersededRepository.countByGrantId(grant.id).assert.isEqualTo(burst.size.toLong() + 1)
    supersededRepository.existsById(stolen.id).assert.isTrue()
    burst.forEach { supersededRepository.existsById(it.id).assert.isTrue() }
  }

  @Test
  fun `a rotation the depth would drop survives while it is younger than the floor`() {
    val now = Instant.now()
    val keep = properties.refreshTokenHistoryGenerations
    val grant = testData.addGrant(refreshExpiresAt = now.plus(Duration.ofDays(20)))
    val recentButDeep = testData.addSupersededRefreshToken(grant, "recent-deep", now.minus(Duration.ofDays(1)))
    (1..keep).forEach { testData.addSupersededRefreshToken(grant, "newer-$it", now.minusSeconds(it.toLong())) }
    testDataService.saveTestData(testData.root)

    authorizationService.pruneRefreshHistoryBeyondDepth()

    supersededRepository.countByGrantId(grant.id).assert.isEqualTo(keep.toLong() + 1)
    supersededRepository.existsById(recentButDeep.id).assert.isTrue()
  }

  @Test
  fun `the bulk grant delete takes a grant's rotation history with it rather than tripping the foreign key`() {
    val now = Instant.now()
    val expired = testData.addGrant(refreshExpiresAt = now.minus(Duration.ofDays(10)))
    val history = testData.addSupersededRefreshToken(expired, "history-of-a-doomed-grant", now.minusSeconds(60))
    testDataService.saveTestData(testData.root)

    authorizationService.deleteExpiredBefore(now.minus(Duration.ofDays(7)))

    repository.existsById(expired.id).assert.isFalse()
    supersededRepository.existsById(history.id).assert.isFalse()
  }

  companion object {
    private const val LIVE_CLIENT = "https://live.example/client"
    private const val ORPHAN_CLIENT = "https://orphan.example/client"
  }
}
