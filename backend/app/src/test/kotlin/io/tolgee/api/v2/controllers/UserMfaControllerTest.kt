package io.tolgee.api.v2.controllers

import io.tolgee.dtos.request.UserMfaRecoveryRequestDto
import io.tolgee.dtos.request.UserTotpDisableRequestDto
import io.tolgee.dtos.request.UserTotpEnableRequestDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.retry
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.UserAccount
import io.tolgee.model.notifications.NotificationType.MFA_DISABLED
import io.tolgee.model.notifications.NotificationType.MFA_ENABLED
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.NotificationTestUtil
import io.tolgee.testing.assertions.Assertions.assertThat
import org.apache.commons.codec.binary.Base32
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Duration
import java.util.Date

class UserMfaControllerTest : AuthorizedControllerTest() {
  companion object {
    private const val TOTP_KEY = "meowmeowmeowmeow"
  }

  private val encodedKey: ByteArray = Base32().decode(TOTP_KEY)

  @Autowired
  private lateinit var notificationUtil: NotificationTestUtil

  private fun enableMfa() {
    userAccount!!.let {
      userAccountService.enableMfaTotp(
        userAccountService.get(it.id),
        encodedKey,
        mfaService.generateStringCode(encodedKey),
      )
    }
    // `enableMfaTotp` bumps `tokens_valid_not_before` - invalidating all tokens.
    // Refresh the token, so we have a new valid one.
    refreshJwtToken()
  }

  private fun enableMfaAtFixedTime() {
    setForcedDate(Date())
    enableMfa()
  }

  private fun persistedUser(): UserAccount = userAccountService.findActive(initialUsername)!!

  @BeforeEach
  fun setUp() {
    notificationUtil.init()
  }

  @AfterEach
  fun tearDown() {
    clearForcedDate()
  }

  @Test
  fun `it enables MFA`() {
    retry {
      val requestDto =
        UserTotpEnableRequestDto(
          totpKey = TOTP_KEY,
          otp = mfaService.generateStringCode(encodedKey),
          password = initialPassword,
        )

      performAuthPut("/v2/user/mfa/totp", requestDto).andIsOk
    }

    assertThat(persistedUser().totpKey).isEqualTo(encodedKey)

    notificationUtil.newestInAppNotification().also {
      assertThat(it.type).isEqualTo(MFA_ENABLED)
      assertThat(it.user.id).isEqualTo(userAccount?.id)
      assertThat(it.originatingUser?.id).isEqualTo(userAccount?.id)
    }
    waitForNotThrowing(timeout = 2000, pollTime = 25) {
      assertThat(
        notificationUtil.newestEmailNotification(),
      ).contains("Multi-factor authentication has been enabled for your account")
    }
  }

  @Test
  fun `it disables MFA`() {
    enableMfa()
    val requestDto =
      UserTotpDisableRequestDto(
        password = initialPassword,
      )
    performAuthDelete("/v2/user/mfa/totp", requestDto).andIsOk
    assertThat(persistedUser().totpKey).isNull()

    notificationUtil.newestInAppNotification().also {
      assertThat(it.type).isEqualTo(MFA_DISABLED)
      assertThat(it.user.id).isEqualTo(userAccount?.id)
      assertThat(it.originatingUser?.id).isEqualTo(userAccount?.id)
    }
    waitForNotThrowing(timeout = 2000, pollTime = 25) {
      assertThat(
        notificationUtil.newestEmailNotification(),
      ).contains("Multi-factor authentication has been disabled for your account")
    }
  }

  @Test
  fun `it regenerates MFA recovery codes`() {
    enableMfa()
    val requestDto =
      UserMfaRecoveryRequestDto(
        password = initialPassword,
      )

    assertThat(persistedUser().mfaRecoveryCodes).isEmpty()

    performAuthPut("/v2/user/mfa/recovery", requestDto).andIsOk
    assertThat(persistedUser().mfaRecoveryCodes).isNotEmpty
  }

  @Test
  fun `it requires valid TOTP code for activation`() {
    retry {
      val requestDto =
        UserTotpEnableRequestDto(
          totpKey = TOTP_KEY,
          otp = (mfaService.generateCode(encodedKey) + 1).toString().padStart(6, '0'),
          password = initialPassword,
        )

      val res =
        performAuthPut("/v2/user/mfa/totp", requestDto)
          .andIsBadRequest
          .andReturn()

      assertThat(res).error().isCustomValidation.hasMessage("invalid_otp_code")
      assertThat(persistedUser().totpKey).isNull()
    }
  }

  @Test
  fun `it requires valid password`() {
    val enableRequestDto =
      UserTotpEnableRequestDto(
        totpKey = TOTP_KEY,
        otp = mfaService.generateStringCode(encodedKey),
        password = "pwease let me innn!!!! >:(",
      )

    val disableRequestDto =
      UserTotpDisableRequestDto(
        password = "pwease let me innn!!!! >:(",
      )

    val recoveryRequestDto =
      UserMfaRecoveryRequestDto(
        password = "pwease let me innn!!!! >:(",
      )

    performAuthPut("/v2/user/mfa/totp", enableRequestDto).andIsForbidden
    assertThat(persistedUser().totpKey).isNull()

    enableMfa()

    loginAsUser(userAccount!!) // get new token

    performAuthDelete("/v2/user/mfa/totp", disableRequestDto).andIsForbidden
    assertThat(persistedUser().totpKey).isNotNull

    performAuthPut("/v2/user/mfa/recovery", recoveryRequestDto).andIsForbidden
    assertThat(persistedUser().mfaRecoveryCodes).isEmpty()
  }

  @Test
  fun `it invalidates tokens generated prior a mfa status change`() {
    retry {
      loginAsAdminIfNotLogged()
      Thread.sleep(1000)

      val enableRequestDto =
        UserTotpEnableRequestDto(
          totpKey = TOTP_KEY,
          otp = mfaService.generateStringCode(encodedKey),
          password = initialPassword,
        )

      performAuthPut("/v2/user/mfa/totp", enableRequestDto).andIsOk
      refreshUser()
      performAuthGet("/v2/user").andExpect(MockMvcResultMatchers.status().isUnauthorized)

      logout()
      loginAsAdminIfNotLogged()
      Thread.sleep(1000)

      val disableRequestDto =
        UserTotpDisableRequestDto(
          password = initialPassword,
        )
      performAuthDelete("/v2/user/mfa/totp", disableRequestDto).andIsOk
      refreshUser()
      performAuthGet("/v2/user").andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }
  }

  @Test
  fun `it accepts TOTP codes from adjacent time steps`() {
    // Code from previous time step should be accepted
    val previousCode = mfaService.generateStringCode(encodedKey, -1)
    assertThat(mfaService.validateTotpCode(encodedKey, previousCode)).isTrue()

    // Code from next time step should be accepted
    val nextCode = mfaService.generateStringCode(encodedKey, 1)
    assertThat(mfaService.validateTotpCode(encodedKey, nextCode)).isTrue()

    // Code from 2 steps away (past) should be rejected
    val tooOldCode = mfaService.generateStringCode(encodedKey, -2)
    assertThat(mfaService.validateTotpCode(encodedKey, tooOldCode)).isFalse()

    // Code from 2 steps away (future) should be rejected
    val tooNewCode = mfaService.generateStringCode(encodedKey, 2)
    assertThat(mfaService.validateTotpCode(encodedKey, tooNewCode)).isFalse()
  }

  @Test
  fun `it rejects replay of the enable-time TOTP code`() {
    enableMfaAtFixedTime()
    val user = persistedUser()
    // Verify the seed value, not just that it was written — a regression that seeds the
    // wrong step (e.g. always the past-drift candidate) would otherwise slip through.
    assertThat(user.totpLastUsedTimeStep).isEqualTo(mfaService.currentTimeStep())

    val enableTimeCode = mfaService.generateStringCode(encodedKey)
    assertThatThrownBy { userAccountService.consumeTotpCode(user, enableTimeCode) }
      .isInstanceOf(AuthenticationException::class.java)
  }

  @Test
  fun `it rejects replay of a successful TOTP code within the same window`() {
    enableMfaAtFixedTime()
    // Move one full step forward so the code we generate below is strictly newer than the
    // counter seeded during enable.
    moveCurrentDate(Duration.ofSeconds(30))

    val code = mfaService.generateStringCode(encodedKey)
    userAccountService.consumeTotpCode(persistedUser(), code) // first use — succeeds

    assertThatThrownBy { userAccountService.consumeTotpCode(persistedUser(), code) }
      .isInstanceOf(AuthenticationException::class.java)
  }

  @Test
  fun `it rejects replay of a past-drift TOTP code`() {
    enableMfaAtFixedTime()
    // Advance past the step used to seed the counter during enable so the -1 candidate
    // becomes a valid future-relative-to-stored step.
    moveCurrentDate(Duration.ofSeconds(60))

    val pastDriftCode = mfaService.generateStringCode(encodedKey, -1)
    userAccountService.consumeTotpCode(persistedUser(), pastDriftCode) // accepted

    assertThatThrownBy { userAccountService.consumeTotpCode(persistedUser(), pastDriftCode) }
      .isInstanceOf(AuthenticationException::class.java)
  }

  @Test
  fun `it burns the current and past windows when future-drift code is accepted`() {
    enableMfaAtFixedTime()
    moveCurrentDate(Duration.ofSeconds(60))

    val futureDriftCode = mfaService.generateStringCode(encodedKey, 1)
    userAccountService.consumeTotpCode(persistedUser(), futureDriftCode) // accepted

    val currentCode = mfaService.generateStringCode(encodedKey, 0)
    assertThatThrownBy { userAccountService.consumeTotpCode(persistedUser(), currentCode) }
      .isInstanceOf(AuthenticationException::class.java)

    val pastCode = mfaService.generateStringCode(encodedKey, -1)
    assertThatThrownBy { userAccountService.consumeTotpCode(persistedUser(), pastCode) }
      .isInstanceOf(AuthenticationException::class.java)
  }

  @Test
  fun `it accepts a new code from the next time step`() {
    enableMfaAtFixedTime()
    moveCurrentDate(Duration.ofSeconds(30))

    val firstCode = mfaService.generateStringCode(encodedKey)
    userAccountService.consumeTotpCode(persistedUser(), firstCode)
    val firstStep = persistedUser().totpLastUsedTimeStep!!

    moveCurrentDate(Duration.ofSeconds(30))
    val nextCode = mfaService.generateStringCode(encodedKey)
    userAccountService.consumeTotpCode(persistedUser(), nextCode)

    // The stored counter must have strictly advanced to the new window's step.
    val nextStep = persistedUser().totpLastUsedTimeStep!!
    assertThat(nextStep).isGreaterThan(firstStep)
    assertThat(nextStep).isEqualTo(mfaService.currentTimeStep())
  }

  @Test
  fun `it resets the counter on disable and re-enable`() {
    enableMfaAtFixedTime()
    // Advance the counter far into the future via a normal authentication.
    moveCurrentDate(Duration.ofSeconds(300))
    userAccountService.consumeTotpCode(persistedUser(), mfaService.generateStringCode(encodedKey))
    val advancedStep = persistedUser().totpLastUsedTimeStep!!

    userAccountService.disableMfaTotp(persistedUser())
    assertThat(persistedUser().totpLastUsedTimeStep).isNull()

    // Re-enable at the SAME wall-clock time. The enable flow must reset + reseed the
    // counter; a subsequent authentication at a later window must succeed.
    enableMfa()
    val seededStep = persistedUser().totpLastUsedTimeStep!!
    assertThat(seededStep).isEqualTo(mfaService.currentTimeStep())

    moveCurrentDate(Duration.ofSeconds(30))
    userAccountService.consumeTotpCode(persistedUser(), mfaService.generateStringCode(encodedKey))
    val postReEnableStep = persistedUser().totpLastUsedTimeStep!!
    assertThat(postReEnableStep).isGreaterThan(seededStep)
    // The post-re-enable flow works regardless of how high the previous counter was.
    assertThat(advancedStep).isLessThan(postReEnableStep + 1L) // sanity: values are comparable
  }

  @Test
  fun `it burns the TOTP window when a recovery code is consumed`() {
    enableMfaAtFixedTime()
    // Provision a known recovery code directly on the user (bypasses the password-protected
    // regeneration endpoint — the unit under test is the replay guard, not the generator).
    val recoveryCode = "aaaa-bbbb-cccc-dddd"
    userAccountService.setMfaRecoveryCodes(persistedUser(), listOf(recoveryCode))

    moveCurrentDate(Duration.ofSeconds(30))
    val burnTimeStep = mfaService.currentTimeStep()
    userAccountService.consumeMfaRecoveryCode(persistedUser(), recoveryCode)

    // The counter must be bumped to exactly `currentTimeStep + 1`, burning the entire ±1
    // window from the recovery-consumption moment.
    assertThat(persistedUser().totpLastUsedTimeStep).isEqualTo(burnTimeStep + 1L)

    // A TOTP code generated at the recovery-consumption time must now be rejected.
    val totpAtRecoveryTime = mfaService.generateStringCode(encodedKey)
    assertThatThrownBy { userAccountService.consumeTotpCode(persistedUser(), totpAtRecoveryTime) }
      .isInstanceOf(AuthenticationException::class.java)
  }

  @Test
  fun `it rejects TOTP consumption for a user without a TOTP key`() {
    loginAsAdminIfNotLogged()
    // Deliberately do NOT call enableMfa — the persisted user has a null `totpKey`.
    assertThatThrownBy { userAccountService.consumeTotpCode(persistedUser(), "123456") }
      .isInstanceOf(AuthenticationException::class.java)
  }

  @Test
  fun `it rejects malformed TOTP codes on the authenticated path`() {
    enableMfaAtFixedTime()

    assertThatThrownBy { userAccountService.consumeTotpCode(persistedUser(), "abcdef") }
      .isInstanceOf(AuthenticationException::class.java)
    assertThatThrownBy { userAccountService.consumeTotpCode(persistedUser(), "12345") }
      .isInstanceOf(AuthenticationException::class.java)
    assertThatThrownBy { userAccountService.consumeTotpCode(persistedUser(), "1234567") }
      .isInstanceOf(AuthenticationException::class.java)
  }

  @Test
  fun `it rejects TOTP replay on the login endpoint`() {
    enableMfaAtFixedTime()
    moveCurrentDate(Duration.ofSeconds(30))

    val code = mfaService.generateStringCode(encodedKey)
    val body = mapOf("username" to initialUsername, "password" to initialPassword, "otp" to code)

    performPost("/api/public/generatetoken", body).andIsOk
    performPost("/api/public/generatetoken", body)
      .andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  fun `it rejects TOTP replay on the super-token endpoint`() {
    enableMfaAtFixedTime()
    moveCurrentDate(Duration.ofSeconds(30))
    // `enableMfa` cleared the JWT-valid-from marker, so refresh the test's auth token.
    loginAsUser(persistedUser())

    val code = mfaService.generateStringCode(encodedKey)
    val body = mapOf("otp" to code)

    performAuthPost("/v2/user/generate-super-token", body).andIsOk
    performAuthPost("/v2/user/generate-super-token", body)
      .andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }
}
