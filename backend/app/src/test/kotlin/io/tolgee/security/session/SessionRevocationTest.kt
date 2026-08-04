package io.tolgee.security.session

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.UserSession
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.model.enums.UserSessionType
import io.tolgee.repository.AuthAuditEventRepository
import io.tolgee.repository.UserSessionRepository
import io.tolgee.security.authentication.UserSessionHotCache
import io.tolgee.service.security.UserSessionService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import java.time.Duration
import java.util.Date
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class SessionRevocationTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var userSessionRepository: UserSessionRepository

  @Autowired
  lateinit var authAuditEventRepository: AuthAuditEventRepository

  @Autowired
  lateinit var userSessionService: UserSessionService

  @Autowired
  lateinit var hotCache: UserSessionHotCache

  lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    // frozen clock: an entry may then only disappear through an explicit eviction
    setForcedDate(Date())
  }

  @AfterEach
  fun cleanup() {
    clearForcedDate()
  }

  @Test
  fun `revoking blocks the token and records one event`() {
    val token = emitToken()
    warmUp(token)

    val session = sessionOf(token)
    userSessionService.revoke(session, testData.user.id)

    performWithToken(token).andIsUnauthorized

    userSessionRepository
      .findByDeviceId(session.deviceId)!!
      .revokedById.assert
      .isEqualTo(testData.user.id)
    revokedEventsFor(session.deviceId).assert.hasSize(1)
  }

  @Test
  fun `eviction happens only after the revocation commits`() {
    val token = emitToken()
    warmUp(token)
    val deviceId = sessionOf(token).deviceId

    executeInNewTransaction {
      userSessionService.revoke(userSessionRepository.findByDeviceId(deviceId)!!, testData.user.id)
      hotCache.get(deviceId).assert.isNotNull
      hotCache
        .get(deviceId)!!
        .revoked.assert
        .isFalse()
    }

    hotCache.get(deviceId).assert.isNull()
  }

  @Test
  fun `revoking others keeps the current session and skips impersonation and foreign rows`() {
    val currentToken = emitToken()
    warmUp(currentToken)
    val otherTokens = (1..3).map { emitToken() }
    otherTokens.forEach { warmUp(it) }
    val otherDeviceIds = otherTokens.map { sessionOf(it).deviceId }

    val impersonation = seedSession(testData.user.id, UserSessionType.IMPERSONATION)
    val otherUser = dbPopulator.createUserIfNotExists("session-isolation@tolgee.io")
    val foreign = seedSession(otherUser.id, UserSessionType.LOGIN_NATIVE)
    val alreadyRevoked = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE, revoked = true)
    val originalRevokedAt = alreadyRevoked.revokedAt!!.time

    userSessionService.revokeAllOthers(
      userAccountId = testData.user.id,
      currentDeviceId = sessionOf(currentToken).deviceId,
      revokedById = testData.user.id,
    )

    performWithToken(currentToken).andIsOk
    otherTokens.forEach { performWithToken(it).andIsUnauthorized }

    userSessionRepository
      .findByDeviceId(impersonation.deviceId)!!
      .revokedAt.assert
      .isNull()
    userSessionRepository
      .findByDeviceId(foreign.deviceId)!!
      .revokedAt.assert
      .isNull()
    userSessionRepository
      .findByDeviceId(alreadyRevoked.deviceId)!!
      .revokedAt!!
      .time
      .assert
      .isEqualTo(originalRevokedAt)

    otherDeviceIds.forEach { revokedEventsFor(it).assert.hasSize(1) }
    revokedEventsFor(impersonation.deviceId).assert.isEmpty()
    revokedEventsFor(foreign.deviceId).assert.isEmpty()
  }

  @Test
  fun `revoking others also sweeps expired and not-before-dead sessions`() {
    val currentToken = emitToken()
    val expired = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE, expired = true)
    val dead = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE, createdLongAgo = true)

    userSessionService.revokeAllOthers(
      userAccountId = testData.user.id,
      currentDeviceId = sessionOf(currentToken).deviceId,
      revokedById = testData.user.id,
    )

    userSessionRepository
      .findByDeviceId(expired.deviceId)!!
      .revokedAt.assert.isNotNull
    userSessionRepository
      .findByDeviceId(dead.deviceId)!!
      .revokedAt.assert.isNotNull
  }

  @Test
  fun `last used writer never resurrects a revoked session`() {
    val session = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE, revoked = true)
    val originalLastUsed = session.lastUsedAt!!.time

    userSessionService.updateLastUsedAsync(session.deviceId, Date(currentDateProvider.date.time + 100000))
    Thread.sleep(1000)

    val reloaded = userSessionRepository.findByDeviceId(session.deviceId)!!
    reloaded.revokedAt.assert.isNotNull
    reloaded.lastUsedAt!!
      .time.assert
      .isEqualTo(originalLastUsed)
  }

  @Test
  fun `backfill leaves an already revoked session revoked`() {
    val session = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE, revoked = true)

    userSessionService.backfillSession(
      deviceId = session.deviceId,
      userAccountId = testData.user.id,
      expiresAt = Date(currentDateProvider.date.time + 100000),
      actingUserAccountId = null,
      ip = null,
      userAgent = null,
    )

    userSessionRepository
      .findByDeviceId(session.deviceId)!!
      .revokedAt.assert.isNotNull
  }

  @Test
  fun `session with a mismatched subject is rejected`() {
    val token = emitToken()
    val session = sessionOf(token)

    val otherUser = dbPopulator.createUserIfNotExists("session-mismatch@tolgee.io")
    executeInNewTransaction {
      val row = userSessionRepository.findByDeviceId(session.deviceId)!!
      row.userAccountId = otherUser.id
      userSessionRepository.save(row)
    }
    hotCache.invalidateAll()

    performWithToken(token).andIsUnauthorized
  }

  @Test
  fun `cache entry expires by write time, not by access time`() {
    val token = emitToken()
    warmUp(token)
    val session = sessionOf(token)

    executeInNewTransaction {
      val row = userSessionRepository.findByDeviceId(session.deviceId)!!
      row.revokedAt = currentDateProvider.date
      userSessionRepository.save(row)
    }

    val ttl = tolgeeProperties.authentication.sessionAudit.sessionCacheTtlMs
    moveCurrentDate(Duration.ofMillis(ttl / 2))
    performWithToken(token).andIsOk

    moveCurrentDate(Duration.ofMillis(ttl / 2 + 1000))
    performWithToken(token).andIsUnauthorized
  }

  @Test
  fun `authenticated requests do not read the session table once cached`() {
    val token = emitToken()
    warmUp(token)
    val deviceId = sessionOf(token).deviceId

    val before = hotCache.get(deviceId)
    before.assert.isNotNull

    repeat(3) { performWithToken(token).andIsOk }

    // still the very same entry: no cold miss happened in between
    hotCache.get(deviceId).assert.isSameAs(before)
  }

  private fun emitToken(): String = jwtService.emitToken(testData.user.id, type = UserSessionType.LOGIN_NATIVE)

  private fun sessionOf(token: String): UserSession {
    val deviceId = jwtService.validateToken(token).deviceId!!
    return userSessionRepository.findByDeviceId(deviceId)!!
  }

  private fun warmUp(token: String) {
    performWithToken(token).andIsOk
  }

  private fun performWithToken(token: String) =
    performGet("/v2/user", HttpHeaders().apply { add("Authorization", "Bearer $token") })

  private fun revokedEventsFor(deviceId: String) =
    authAuditEventRepository.findAll().filter {
      it.type == AuthAuditEventType.SESSION_REVOKED && it.deviceId == deviceId
    }

  private fun seedSession(
    userAccountId: Long,
    type: UserSessionType,
    revoked: Boolean = false,
    expired: Boolean = false,
    createdLongAgo: Boolean = false,
  ): UserSession {
    val now = currentDateProvider.date
    return executeInNewTransaction {
      userSessionRepository.save(
        UserSession().apply {
          this.deviceId = UUID.randomUUID().toString()
          this.userAccountId = userAccountId
          this.type = type
          this.expiresAt = expiredOrFuture(expired, now)
          this.lastUsedAt = now
          if (revoked) {
            this.revokedAt = Date(now.time - 60000)
            this.revokedById = userAccountId
          }
          if (createdLongAgo) {
            this.lastRefreshedAt = Date(now.time - 3600000)
          }
        },
      )
    }
  }

  private fun expiredOrFuture(
    expired: Boolean,
    now: Date,
  ): Date {
    if (expired) return Date(now.time - 60000)
    return Date(now.time + 3600000)
  }
}
