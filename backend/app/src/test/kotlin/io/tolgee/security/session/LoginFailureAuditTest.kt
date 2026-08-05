package io.tolgee.security.session

import io.tolgee.component.bucket.TokenBucketManager
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.AuthAuditEvent
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.repository.AuthAuditEventRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.apache.commons.codec.binary.Base32
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.util.Date
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class LoginFailureAuditTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var authAuditEventRepository: AuthAuditEventRepository

  lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    // the login bucket is a JVM-wide singleton that nothing else resets between tests
    TokenBucketManager.localTokenBucketStorage.clear()
  }

  @AfterEach
  fun cleanup() {
    TokenBucketManager.localTokenBucketStorage.clear()
    clearForcedDate()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `unknown user is recorded with the attempted identity and no user`() {
    val username = "ghost-${UUID.randomUUID()}@tolgee.io"
    attemptLogin(username, "whatever").andIsUnauthorized

    val event = singleEvent()
    event.type.assert.isEqualTo(AuthAuditEventType.LOGIN_FAILED_BAD_CREDENTIALS)
    event.attemptedUsername.assert.isEqualTo(username)
    event.userAccountId.assert.isNull()
    event.ip.assert.isEqualTo(TEST_IP)
    event.userAgent.assert.isEqualTo(TEST_USER_AGENT)
  }

  @Test
  fun `wrong password is recorded against the resolved user`() {
    attemptLogin(testData.user.username, "definitely-not-the-password").andIsUnauthorized

    val event = singleEvent()
    event.type.assert.isEqualTo(AuthAuditEventType.LOGIN_FAILED_BAD_CREDENTIALS)
    event.userAccountId.assert.isEqualTo(testData.user.id)
    event.data
      ?.get("reason")
      .assert
      .isEqualTo("BAD_CREDENTIALS")
  }

  @Test
  fun `over-long username is truncated rather than breaking the request`() {
    val prefix = "long-${UUID.randomUUID()}"
    val username = prefix + "x".repeat(400)
    attemptLogin(username, "whatever").andIsUnauthorized

    val event = singleEvent()
    event.attemptedUsername!!
      .length.assert
      .isEqualTo(255)
    event.attemptedUsername!!
      .startsWith(prefix)
      .assert
      .isTrue()
  }

  @Test
  fun `the MFA challenge is not recorded as a failed login`() {
    enableMfa()

    attemptLogin(testData.user.username, "admin").andIsUnauthorized

    failureEvents().assert.isEmpty()
  }

  @Test
  fun `a wrong OTP is recorded as an invalid OTP`() {
    enableMfa()

    attemptLogin(testData.user.username, "admin", otp = "000000").andIsUnauthorized

    val event = singleEvent()
    event.type.assert.isEqualTo(AuthAuditEventType.LOGIN_FAILED_INVALID_OTP)
    event.userAccountId.assert.isEqualTo(testData.user.id)
  }

  private fun enableMfa() {
    val encodedKey = Base32().decode(TOTP_KEY)
    userAccountService.enableMfaTotp(
      userAccountService.get(testData.user.id),
      encodedKey,
      mfaService.generateStringCode(encodedKey),
    )
  }

  private fun singleEvent(): AuthAuditEvent {
    val events = failureEvents()
    events.assert.hasSize(1)
    return events.first()
  }

  private fun failureEvents() =
    authAuditEventRepository.findAll().filter {
      it.type.name.startsWith("LOGIN_FAILED")
    }

  private fun attemptLogin(
    username: String,
    password: String,
    otp: String? = null,
  ): ResultActions {
    val body = mutableMapOf<String, Any>("username" to username, "password" to password)
    otp?.let { body["otp"] = it }
    return perform(
      MockMvcRequestBuilders
        .post("/api/public/generatetoken")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Forwarded-For", TEST_IP)
        .header("User-Agent", TEST_USER_AGENT)
        .content(mapper.writeValueAsString(body)),
    )
  }

  companion object {
    const val TEST_IP = "203.0.113.7"
    const val TEST_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1.15"
    const val TOTP_KEY = "meowmeowmeowmeow"
  }
}
