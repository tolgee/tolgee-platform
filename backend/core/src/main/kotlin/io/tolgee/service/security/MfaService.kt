package io.tolgee.service.security

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator
import io.tolgee.api.isMfaEnabled
import io.tolgee.component.KeyGenerator
import io.tolgee.configuration.SecurityConfiguration
import io.tolgee.constants.Message
import io.tolgee.dtos.request.UserMfaRecoveryRequestDto
import io.tolgee.dtos.request.UserTotpDisableRequestDto
import io.tolgee.dtos.request.UserTotpEnableRequestDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.UserAccount
import org.apache.commons.codec.binary.Base32
import org.springframework.stereotype.Service
import java.time.Instant
import javax.crypto.spec.SecretKeySpec

@Service
class MfaService(
  private val totpGenerator: TimeBasedOneTimePasswordGenerator,
  private val userAccountService: UserAccountService,
  private val userCredentialsService: UserCredentialsService,
  private val keyGenerator: KeyGenerator,
) {
  private val base32 = Base32()

  fun enableTotpFor(
    user: UserAccount,
    dto: UserTotpEnableRequestDto,
  ) {
    try {
      userCredentialsService.checkUserCredentials(user, dto.password)
    } catch (e: AuthenticationException) {
      // Re-throw as a permission exception to set status to 403 instead of 401
      throw PermissionException(Message.BAD_CREDENTIALS)
    }

    if (user.isMfaEnabled) {
      throw BadRequestException(Message.MFA_ENABLED)
    }

    val key = base32.decode(dto.totpKey)
    if (!validateTotpCode(key, dto.otp)) {
      throw ValidationException(Message.INVALID_OTP_CODE)
    }

    userAccountService.enableMfaTotp(user, key)
  }

  fun disableTotpFor(
    user: UserAccount,
    dto: UserTotpDisableRequestDto,
  ) {
    try {
      userCredentialsService.checkUserCredentials(user, dto.password)
    } catch (e: AuthenticationException) {
      // Re-throw as a permission exception to set status to 403 instead of 401
      throw PermissionException(Message.BAD_CREDENTIALS)
    }

    if (user.totpKey?.isNotEmpty() != true) {
      throw BadRequestException(Message.MFA_NOT_ENABLED)
    }

    userAccountService.disableMfaTotp(user)
  }

  fun regenerateRecoveryCodes(
    user: UserAccount,
    dto: UserMfaRecoveryRequestDto,
  ): List<String> {
    try {
      userCredentialsService.checkUserCredentials(user, dto.password)
    } catch (e: AuthenticationException) {
      // Re-throw as a permission exception to set status to 403 instead of 401
      throw PermissionException(Message.BAD_CREDENTIALS)
    }

    if (!hasMfaEnabled(user)) {
      throw BadRequestException(Message.MFA_NOT_ENABLED)
    }

    val codes =
      List(10) {
        val key = keyGenerator.generate()
        "${key.substring(0, 4)}-${key.substring(4, 8)}-${key.substring(8, 12)}-${key.substring(12, 16)}"
      }

    userAccountService.setMfaRecoveryCodes(user, codes)
    return codes
  }

  fun hasMfaEnabled(user: UserAccount): Boolean {
    return mfaEnabled(user.totpKey)
  }

  fun checkMfa(
    user: UserAccount,
    otp: String?,
  ) {
    if (!user.isMfaEnabled) {
      return
    }

    if (otp?.isEmpty() != false) {
      throw AuthenticationException(Message.MFA_ENABLED)
    }

    if (otp.length == 6) {
      if (!validateTotpCode(user, otp)) {
        throw AuthenticationException(Message.INVALID_OTP_CODE)
      }
    } else {
      userAccountService.consumeMfaRecoveryCode(user, otp)
    }
  }

  fun validateTotpCode(
    user: UserAccount,
    code: String,
  ): Boolean {
    if (user.totpKey?.isNotEmpty() != true) return true
    return validateTotpCode(user.totpKey!!, code)
  }

  fun validateTotpCode(
    key: ByteArray,
    code: String,
  ): Boolean {
    if (code.length != 6) return false

    return try {
      val parsedCode = code.toInt()
      val expectedCode = generateCode(key)
      parsedCode == expectedCode
    } catch (e: NumberFormatException) {
      false
    }
  }

  fun generateCode(key: ByteArray): Int {
    return totpGenerator.generateOneTimePassword(SecretKeySpec(key, SecurityConfiguration.OTP_ALGORITHM), Instant.now())
  }

  fun generateStringCode(key: ByteArray): String {
    return generateCode(key).toString().padStart(6, '0')
  }

  companion object {
    fun mfaEnabled(totpKeyOrNull: ByteArray?): Boolean {
      return totpKeyOrNull?.isNotEmpty() == true
    }
  }
}
