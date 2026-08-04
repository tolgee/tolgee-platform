package io.tolgee.ee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.node
import io.tolgee.model.UserSession
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.model.enums.UserSessionType
import io.tolgee.repository.AuthAuditEventRepository
import io.tolgee.repository.UserSessionRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import java.util.Date
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class UserSessionsControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var userSessionRepository: UserSessionRepository

  @Autowired
  lateinit var authAuditEventRepository: AuthAuditEventRepository

  lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    setForcedDate(Date())
  }

  @AfterEach
  fun cleanup() {
    clearForcedDate()
  }

  @Test
  fun `lists only the callers own live sessions`() {
    val listed = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE)
    val revoked = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE, revoked = true)
    val expired = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE, expired = true)
    val impersonation = seedSession(testData.user.id, UserSessionType.IMPERSONATION)
    val otherUser = dbPopulator.createUserIfNotExists("sessions-controller-other@tolgee.io")
    val foreign = seedSession(otherUser.id, UserSessionType.LOGIN_NATIVE)

    val ids = listedIds()
    ids.assert.contains(listed.id)
    ids.assert.doesNotContain(revoked.id, expired.id, impersonation.id, foreign.id)
  }

  @Test
  fun `marks the session of the calling token as current`() {
    performAuthGet("/v2/user/sessions").andIsOk.andAssertThatJson {
      node("_embedded.sessions") {
        node("[0].isCurrent").isEqualTo(true)
      }
    }
  }

  @Test
  fun `exposes the expiry of a session`() {
    val session = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE)

    performAuthGet("/v2/user/sessions").andIsOk.andAssertThatJson {
      node("_embedded.sessions").isArray.anySatisfy {
        it.toString().contains(session.expiresAt.time.toString())
      }
    }
  }

  @Test
  fun `revokes a session and stays idempotent`() {
    val session = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE)

    performAuthDelete("/v2/user/sessions/${session.id}").andIsOk
    val revokedAt = userSessionRepository.findById(session.id).get().revokedAt
    revokedAt.assert.isNotNull

    performAuthDelete("/v2/user/sessions/${session.id}").andIsOk
    userSessionRepository
      .findById(session.id)
      .get()
      .revokedAt!!
      .time.assert
      .isEqualTo(revokedAt!!.time)
    revokedEventsFor(session.deviceId).assert.hasSize(1)
  }

  @Test
  fun `reports impersonation sessions and unknown ids as missing`() {
    val impersonation = seedSession(testData.user.id, UserSessionType.IMPERSONATION)

    performAuthDelete("/v2/user/sessions/${impersonation.id}").andIsNotFound
    performAuthDelete("/v2/user/sessions/${impersonation.id + 999999}").andIsNotFound
  }

  @Test
  fun `refuses to revoke a session of another user`() {
    val otherUser = dbPopulator.createUserIfNotExists("sessions-controller-foreign@tolgee.io")
    val foreign = seedSession(otherUser.id, UserSessionType.LOGIN_NATIVE)

    performAuthDelete("/v2/user/sessions/${foreign.id}").andIsForbidden
  }

  @Test
  fun `revoking others keeps the current session and leaves other users alone`() {
    val mine = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE)
    val otherUser = dbPopulator.createUserIfNotExists("sessions-controller-untouched@tolgee.io")
    val foreign = seedSession(otherUser.id, UserSessionType.LOGIN_NATIVE)

    performAuthDelete("/v2/user/sessions/other").andIsOk

    userSessionRepository
      .findById(mine.id)
      .get()
      .revokedAt.assert.isNotNull
    userSessionRepository
      .findById(foreign.id)
      .get()
      .revokedAt.assert
      .isNull()
    revokedEventsFor(foreign.deviceId).assert.isEmpty()
    // the caller can still use its own token
    performAuthGet("/v2/user/sessions").andIsOk
  }

  @Test
  fun `revoking the current session ends it`() {
    performAuthGet("/v2/user/sessions").andIsOk
    performAuthDelete("/v2/user/sessions/current").andIsOk

    performAuthGet("/v2/user/sessions").andIsUnauthorized
  }

  @Test
  fun `session management stays reachable without a super token`() {
    val token = jwtService.emitToken(testData.user.id, type = UserSessionType.LOGIN_NATIVE, isSuper = false)
    val session = seedSession(testData.user.id, UserSessionType.LOGIN_NATIVE)

    // ordered so that revoking the caller's own session comes last
    performWithToken(org.springframework.http.HttpMethod.GET, "/v2/user/sessions", token).andIsOk
    performWithToken(
      org.springframework.http.HttpMethod.DELETE,
      "/v2/user/sessions/${session.id}",
      token,
    ).andIsOk
    performWithToken(org.springframework.http.HttpMethod.DELETE, "/v2/user/sessions/other", token).andIsOk
    performWithToken(org.springframework.http.HttpMethod.DELETE, "/v2/user/sessions/current", token).andIsOk
  }

  @Test
  fun `sessions dropped by a password change disappear from the listing`() {
    val stale =
      seedSession(
        testData.user.id,
        UserSessionType.LOGIN_NATIVE,
        createdAt =
          Date(currentDateProvider.date.time - 3600000),
      )
    listedIds().assert.contains(stale.id)

    executeInNewTransaction {
      userAccountService.invalidateTokens(
        userAccountService.get(testData.user.id),
        io.tolgee.model.enums.AllTokensInvalidatedTrigger.PASSWORD_CHANGE,
      )
    }
    // the caller's own token has to survive the bump to keep listing
    moveCurrentDate(java.time.Duration.ofSeconds(2))

    listedIds().assert.doesNotContain(stale.id)
    authAuditEventRepository
      .findAll()
      .filter { it.type == AuthAuditEventType.ALL_TOKENS_INVALIDATED && it.userAccountId == testData.user.id }
      .assert
      .isNotEmpty
  }

  private fun listedIds(): List<Long> {
    val body =
      performAuthGet("/v2/user/sessions?size=100")
        .andIsOk
        .andReturn()
        .response.contentAsString
    val embedded = mapper.readTree(body)["_embedded"] ?: return emptyList()
    val sessions = embedded["sessions"] ?: return emptyList()
    return sessions.map { it["id"].asLong() }
  }

  private fun revokedEventsFor(deviceId: String) =
    authAuditEventRepository.findAll().filter {
      it.type == AuthAuditEventType.SESSION_REVOKED && it.deviceId == deviceId
    }

  private fun performWithToken(
    method: org.springframework.http.HttpMethod,
    url: String,
    token: String,
  ) = perform(
    org.springframework.test.web.servlet.request.MockMvcRequestBuilders
      .request(method, url)
      .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
  )

  private fun seedSession(
    userAccountId: Long,
    type: UserSessionType,
    revoked: Boolean = false,
    expired: Boolean = false,
    createdAt: Date? = null,
  ): UserSession {
    val now = currentDateProvider.date
    return executeInNewTransaction {
      val saved =
        userSessionRepository.save(
          UserSession().apply {
            this.deviceId = UUID.randomUUID().toString()
            this.userAccountId = userAccountId
            this.type = type
            this.expiresAt = expiryFor(expired, now)
            this.lastUsedAt = now
            if (revoked) {
              this.revokedAt = now
              this.revokedById = userAccountId
            }
          },
        )
      createdAt?.let {
        entityManager
          .createNativeQuery("update user_session set created_at = :date where id = :id")
          .setParameter("date", it)
          .setParameter("id", saved.id)
          .executeUpdate()
      }
      saved
    }
  }

  private fun expiryFor(
    expired: Boolean,
    now: Date,
  ): Date {
    if (expired) return Date(now.time - 60000)
    return Date(now.time + 3600000)
  }
}
