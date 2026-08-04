package io.tolgee.security.session

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.model.enums.UserSessionType
import io.tolgee.repository.AuthAuditEventRepository
import io.tolgee.repository.UserSessionRepository
import io.tolgee.service.security.UserSessionService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.Date
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class UserSessionLifecycleTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var userSessionRepository: UserSessionRepository

  @Autowired
  lateinit var authAuditEventRepository: AuthAuditEventRepository

  @Autowired
  lateinit var userSessionService: UserSessionService

  lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    RequestContextHolder.resetRequestAttributes()
    clearForcedDate()
  }

  @Test
  fun `native login creates a session and a login event`() {
    val response = doAuthentication(testData.user.username, "admin")
    response.andIsOk

    val sessions = userSessionRepository.findAll().filter { it.userAccountId == testData.user.id }
    sessions.assert.hasSize(1)
    sessions
      .first()
      .type.assert
      .isEqualTo(UserSessionType.LOGIN_NATIVE)

    val events = eventsOf(testData.user.id, AuthAuditEventType.LOGIN)
    events.assert.hasSize(1)
    events
      .first()
      .data
      ?.get("sessionType")
      .assert
      .isEqualTo(UserSessionType.LOGIN_NATIVE.name)
  }

  @Test
  fun `records ip and user agent from the request`() {
    performLoginWithHeaders("10.11.12.13", "Mozilla/5.0 (X11; Linux x86_64) Firefox/119.0")

    val session = userSessionRepository.findAll().first { it.userAccountId == testData.user.id }
    session.ip.assert.isEqualTo("10.11.12.13")
    session.userAgent.assert.isEqualTo("Mozilla/5.0 (X11; Linux x86_64) Firefox/119.0")
  }

  @Test
  fun `truncates over-long ip and user agent instead of failing`() {
    performLoginWithHeaders("1".repeat(300), "u".repeat(600))

    val session = userSessionRepository.findAll().first { it.userAccountId == testData.user.id }
    session.ip!!
      .length.assert
      .isEqualTo(64)
    session.userAgent!!
      .length.assert
      .isEqualTo(255)
  }

  @Test
  fun `refresh updates the same session and keeps its type`() {
    val deviceId = UUID.randomUUID().toString()
    withRequestContext {
      jwtService.emitToken(testData.user.id, type = UserSessionType.LOGIN_GITHUB, refreshedDeviceId = deviceId)
    }

    val created = userSessionRepository.findByDeviceId(deviceId)!!
    val originalExpiry = created.expiresAt

    moveCurrentDate(java.time.Duration.ofMinutes(5))
    withRequestContext {
      jwtService.emitToken(
        testData.user.id,
        type = UserSessionType.UNKNOWN,
        refreshedDeviceId = deviceId,
        isRefresh = true,
      )
    }

    val sessions = userSessionRepository.findAll().filter { it.deviceId == deviceId }
    sessions.assert.hasSize(1)
    val refreshed = sessions.first()
    refreshed.type.assert.isEqualTo(UserSessionType.LOGIN_GITHUB)
    refreshed.lastRefreshedAt.assert.isNotNull
    refreshed.expiresAt.time.assert
      .isGreaterThan(originalExpiry.time)

    val refreshEvents = eventsOf(testData.user.id, AuthAuditEventType.TOKEN_REFRESH)
    refreshEvents.assert.hasSize(1)
    refreshEvents
      .first()
      .data
      ?.get("sessionType")
      .assert
      .isEqualTo(UserSessionType.LOGIN_GITHUB.name)
  }

  @Test
  fun `refresh with no existing row inserts it as unknown`() {
    val deviceId = UUID.randomUUID().toString()

    withRequestContext {
      userSessionService.registerToken(
        deviceId = deviceId,
        userAccountId = testData.user.id,
        // deliberately not UNKNOWN - the insert must ignore it on the refresh path
        type = UserSessionType.LOGIN_NATIVE,
        actingUserAccountId = null,
        expiresAt = Date(currentDateProvider.date.time + 10000),
        isRefresh = true,
      )
    }

    userSessionRepository
      .findByDeviceId(deviceId)!!
      .type.assert
      .isEqualTo(UserSessionType.UNKNOWN)
    eventsOf(testData.user.id, AuthAuditEventType.TOKEN_REFRESH)
      .first()
      .data
      ?.get("sessionType")
      .assert
      .isEqualTo(UserSessionType.UNKNOWN.name)
  }

  @Test
  fun `upsert never overwrites type, creation, revocation or last used`() {
    val deviceId = UUID.randomUUID().toString()
    val createdAtMark = Date(currentDateProvider.date.time - 100000)
    executeInNewTransaction {
      val session =
        io.tolgee.model.UserSession().apply {
          this.deviceId = deviceId
          this.userAccountId = testData.user.id
          this.type = UserSessionType.LOGIN_GITHUB
          this.ip = "1.1.1.1"
          this.userAgent = "old-agent"
          this.expiresAt = Date(currentDateProvider.date.time + 1000)
          this.lastUsedAt = createdAtMark
          this.revokedAt = createdAtMark
          this.revokedById = testData.user.id
          this.actingUserAccountId = testData.user.id
        }
      userSessionRepository.save(session)
    }

    val newExpiry = Date(currentDateProvider.date.time + 999000)
    withRequestContext(ip = "2.2.2.2", userAgent = "new-agent") {
      userSessionService.registerToken(
        deviceId = deviceId,
        userAccountId = testData.user.id,
        type = UserSessionType.LOGIN_NATIVE,
        actingUserAccountId = null,
        expiresAt = newExpiry,
        isRefresh = false,
      )
    }

    val rows = userSessionRepository.findAll().filter { it.deviceId == deviceId }
    rows.assert.hasSize(1)
    val row = rows.first()
    row.type.assert.isEqualTo(UserSessionType.LOGIN_GITHUB)
    row.revokedAt.assert.isNotNull
    row.revokedById.assert.isEqualTo(testData.user.id)
    row.actingUserAccountId.assert.isEqualTo(testData.user.id)
    row.lastUsedAt!!
      .time.assert
      .isEqualTo(createdAtMark.time)
    // not a refresh, so the refresh stamp must stay untouched
    row.lastRefreshedAt.assert.isNull()
    row.ip.assert.isEqualTo("2.2.2.2")
    row.userAgent.assert.isEqualTo("new-agent")
    row.expiresAt.time.assert
      .isEqualTo(newExpiry.time)
  }

  @Test
  fun `rolled back token emission leaves no session and no event`() {
    val deviceId = UUID.randomUUID().toString()

    val thrown =
      runCatching {
        executeInNewTransaction {
          jwtService.emitToken(testData.user.id, type = UserSessionType.LOGIN_NATIVE, refreshedDeviceId = deviceId)
          throw TestRollbackException()
        }
      }
    thrown.isFailure.assert.isTrue()

    userSessionRepository.findByDeviceId(deviceId).assert.isNull()
    authAuditEventRepository
      .findAll()
      .filter { it.deviceId == deviceId }
      .assert
      .isEmpty()
  }

  @Test
  fun `sessions cascade away with the user while audit events survive`() {
    val user = dbPopulator.createUserIfNotExists("audit-survivor@tolgee.io")
    withRequestContext {
      jwtService.emitToken(user.id, type = UserSessionType.LOGIN_NATIVE)
    }
    eventsOf(user.id, AuthAuditEventType.LOGIN).assert.hasSize(1)

    executeInNewTransaction {
      entityManager
        .createNativeQuery("delete from user_account where id = :id")
        .setParameter("id", user.id)
        .executeUpdate()
    }

    userSessionRepository
      .findAll()
      .filter { it.userAccountId == user.id }
      .assert
      .isEmpty()
    eventsOf(user.id, AuthAuditEventType.LOGIN).assert.hasSize(1)
  }

  @Test
  fun `last used write is debounced and touches nothing else`() {
    setForcedDate(Date())
    val token = loginAndGetToken()
    val deviceId = deviceIdOf(token)

    // a session this fresh counts as just used, so nothing is written yet
    performWithToken(token)
    Thread.sleep(500)
    userSessionRepository
      .findByDeviceId(deviceId)!!
      .lastUsedAt.assert
      .isNull()

    val before = userSessionRepository.findByDeviceId(deviceId)!!

    // still inside the debounce interval
    moveCurrentDate(java.time.Duration.ofSeconds(30))
    performWithToken(token)
    Thread.sleep(500)
    userSessionRepository
      .findByDeviceId(deviceId)!!
      .lastUsedAt.assert
      .isNull()

    moveCurrentDate(java.time.Duration.ofMinutes(6))
    performWithToken(token)
    waitForNotThrowing(throwableClass = AssertionError::class, timeout = 5000) {
      executeInNewTransaction {
        userSessionRepository
          .findByDeviceId(deviceId)!!
          .lastUsedAt.assert.isNotNull
      }
    }

    val after = userSessionRepository.findByDeviceId(deviceId)!!
    after.ip.assert.isEqualTo(before.ip)
    after.userAgent.assert.isEqualTo(before.userAgent)
    after.type.assert.isEqualTo(before.type)
    after.revokedAt.assert.isNull()
  }

  private fun eventsOf(
    userAccountId: Long,
    type: AuthAuditEventType,
  ) = authAuditEventRepository.findAll().filter { it.userAccountId == userAccountId && it.type == type }

  private fun loginAndGetToken(): String {
    val result = doAuthentication(testData.user.username, "admin")
    val body = result.andReturn().response.contentAsString
    return mapper.readTree(body)["accessToken"].asText()
  }

  private fun deviceIdOf(token: String): String {
    val auth = jwtService.validateToken(token)
    return auth.deviceId!!
  }

  private fun performWithToken(token: String) {
    performGet(
      "/v2/user",
      org.springframework.http
        .HttpHeaders()
        .apply { add("Authorization", "Bearer $token") },
    ).andIsOk
  }

  private fun performLoginWithHeaders(
    ip: String,
    userAgent: String,
  ) {
    perform(
      org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .post("/api/public/generatetoken")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .header("X-Forwarded-For", ip)
        .header("User-Agent", userAgent)
        .content(
          mapper.writeValueAsString(
            mapOf("username" to testData.user.username, "password" to "admin"),
          ),
        ),
    ).andIsOk
  }

  private fun <T> withRequestContext(
    ip: String? = null,
    userAgent: String? = null,
    fn: () -> T,
  ): T {
    val request = MockHttpServletRequest()
    ip?.let { request.addHeader("X-Forwarded-For", it) }
    userAgent?.let { request.addHeader("User-Agent", it) }
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    try {
      return fn()
    } finally {
      RequestContextHolder.resetRequestAttributes()
    }
  }

  private class TestRollbackException : RuntimeException("rollback")
}
