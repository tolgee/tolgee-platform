package io.tolgee.api.v2.controllers

import io.tolgee.dtos.request.UserMfaRecoveryRequestDto
import io.tolgee.dtos.request.UserTotpDisableRequestDto
import io.tolgee.dtos.request.UserTotpEnableRequestDto
import io.tolgee.service.MfaService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.apache.commons.codec.binary.Base32
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class UserMfaControllerTest : AuthorizedControllerTest() {
  companion object {
    private const val TOTP_KEY = "meowmeowmeowmeow"
  }

  @Autowired
  lateinit var mfaService: MfaService

  private val encodedKey: ByteArray = Base32().decode(TOTP_KEY)

  fun enableMfa() {
    userAccount?.let {
      userAccountService.enableMfaTotp(it, encodedKey)
    }
  }

  @Test
  fun `it enables MFA`() {
    val requestDto = UserTotpEnableRequestDto(
      totpKey = TOTP_KEY,
      otp = mfaService.generateCode(encodedKey).toString(),
      password = initialPassword
    )

    performAuthPut("/v2/user/mfa/totp", requestDto).andExpect(MockMvcResultMatchers.status().isOk)
    val fromDb = userAccountService.findOptional(initialUsername)
    Assertions.assertThat(fromDb).isNotEmpty
    Assertions.assertThat(fromDb.get().totpKey).isEqualTo(encodedKey)
  }

  @Test
  fun `it disables MFA`() {
    enableMfa()
    val requestDto = UserTotpDisableRequestDto(
      password = initialPassword
    )
    performAuthDelete("/v2/user/mfa/totp", requestDto).andExpect(MockMvcResultMatchers.status().isOk)
    val fromDb = userAccountService.findOptional(initialUsername)
    Assertions.assertThat(fromDb).isNotEmpty
    Assertions.assertThat(fromDb.get().totpKey).isNull()
  }

  @Test
  fun `it regenerates MFA recovery codes`() {
    enableMfa()
    val requestDto = UserMfaRecoveryRequestDto(
      password = initialPassword
    )

    var fromDb = userAccountService.findOptional(initialUsername)
    Assertions.assertThat(fromDb).isNotEmpty
    Assertions.assertThat(fromDb.get().mfaRecoveryCodes).isEmpty()

    performAuthPut("/v2/user/mfa/recovery", requestDto).andExpect(MockMvcResultMatchers.status().isOk)
    fromDb = userAccountService.findOptional(initialUsername)
    Assertions.assertThat(fromDb).isNotEmpty
    Assertions.assertThat(fromDb.get().mfaRecoveryCodes).isNotEmpty
  }

  @Test
  fun `it requires valid TOTP code for activation`() {
    val requestDto = UserTotpEnableRequestDto(
      totpKey = TOTP_KEY,
      otp = (mfaService.generateCode(encodedKey) + 1).toString(),
      password = initialPassword
    )

    val res = performAuthPut("/v2/user/mfa/totp", requestDto)
      .andExpect(MockMvcResultMatchers.status().isBadRequest)
      .andReturn()

    assertThat(res).error().isCustomValidation.hasMessage("invalid_otp_code")
    val fromDb = userAccountService.findOptional(initialUsername)
    Assertions.assertThat(fromDb).isNotEmpty
    Assertions.assertThat(fromDb.get().totpKey).isNull()
  }

  @Test
  fun `it requires valid password`() {
    val enableRequestDto = UserTotpEnableRequestDto(
      totpKey = TOTP_KEY,
      otp = mfaService.generateCode(encodedKey).toString(),
      password = "pwease let me innn!!!! >:("
    )

    val disableRequestDto = UserTotpEnableRequestDto(
      password = "pwease let me innn!!!! >:("
    )

    val recoveryRequestDto = UserMfaRecoveryRequestDto(
      password = "pwease let me innn!!!! >:("
    )

    performAuthPut("/v2/user/mfa/totp", enableRequestDto).andExpect(MockMvcResultMatchers.status().isForbidden)
    var fromDb = userAccountService.findOptional(initialUsername)
    Assertions.assertThat(fromDb).isNotEmpty
    Assertions.assertThat(fromDb.get().totpKey).isNull()

    enableMfa()

    performAuthDelete("/v2/user/mfa/totp", disableRequestDto).andExpect(MockMvcResultMatchers.status().isForbidden)
    fromDb = userAccountService.findOptional(initialUsername)
    Assertions.assertThat(fromDb).isNotEmpty
    Assertions.assertThat(fromDb.get().totpKey).isNotNull

    performAuthPut("/v2/user/mfa/recovery", recoveryRequestDto).andExpect(MockMvcResultMatchers.status().isForbidden)
    fromDb = userAccountService.findOptional(initialUsername)
    Assertions.assertThat(fromDb).isNotEmpty
    Assertions.assertThat(fromDb.get().mfaRecoveryCodes).isEmpty()
  }

  @Test
  fun `it invalidates tokens generated prior a mfa status change`() {
    val enableRequestDto = UserTotpEnableRequestDto(
      totpKey = TOTP_KEY,
      otp = mfaService.generateCode(encodedKey).toString(),
      password = initialPassword
    )

    performAuthPut("/v2/user/mfa/totp", enableRequestDto).andExpect(MockMvcResultMatchers.status().isOk)
    performAuthGet("/v2/user").andExpect(MockMvcResultMatchers.status().isUnauthorized)
    logout()

    val disableRequestDto = UserTotpDisableRequestDto(
      password = initialPassword
    )
    performAuthDelete("/v2/user/mfa/totp", disableRequestDto).andExpect(MockMvcResultMatchers.status().isOk)
    performAuthGet("/v2/user").andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }
}
