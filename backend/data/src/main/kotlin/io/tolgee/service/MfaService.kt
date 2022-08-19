package io.tolgee.service

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator
import io.tolgee.configuration.MfaConfiguration
import io.tolgee.constants.Message
import io.tolgee.dtos.request.UserMfaRecoveryRequestDto
import io.tolgee.dtos.request.UserTotpDisableRequestDto
import io.tolgee.dtos.request.UserTotpEnableRequestDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import org.apache.commons.codec.binary.Base32
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Service
class MfaService(
  private val userAccountService: UserAccountService,
  private val totpGenerator: TimeBasedOneTimePasswordGenerator
) {
  private val base32 = Base32()

  fun enableTotpFor(user: UserAccount, dto: UserTotpEnableRequestDto) {
    if (user.totpKey?.isNotEmpty() == true) {
      throw AuthenticationException(Message.MFA_ENABLED)
    }

    val bCryptPasswordEncoder = BCryptPasswordEncoder()
    val matches = bCryptPasswordEncoder.matches(dto.password, user.password)
    if (!matches) {
      throw AuthenticationException(Message.BAD_CREDENTIALS)
    }

    val key = base32.decode(dto.totpKey)
    if (!validateTotpCode(key, dto.otp)) {
      throw AuthenticationException(Message.INVALID_OTP_CODE)
    }

    userAccountService.enableMfaTotp(user, key)
  }

  fun disableTotpFor(user: UserAccount, dto: UserTotpDisableRequestDto) {
    if (user.totpKey?.isNotEmpty() != true) {
      throw AuthenticationException(Message.MFA_NOT_ENABLED)
    }

    val bCryptPasswordEncoder = BCryptPasswordEncoder()
    val matches = bCryptPasswordEncoder.matches(dto.password, user.password)
    if (!matches) {
      throw AuthenticationException(Message.BAD_CREDENTIALS)
    }

    userAccountService.disableMfaTotp(user)
  }

  fun regenerateRecoveryCodes(user: UserAccount, dto: UserMfaRecoveryRequestDto): List<String> {
    if (!hasMfaEnabled(user)) {
      throw AuthenticationException(Message.MFA_NOT_ENABLED)
    }

    val bCryptPasswordEncoder = BCryptPasswordEncoder()
    val matches = bCryptPasswordEncoder.matches(dto.password, user.password)
    if (!matches) {
      throw AuthenticationException(Message.BAD_CREDENTIALS)
    }

    val codes = List(10) { UUID.randomUUID().toString() }
    userAccountService.setMfaRecoveryCodes(user, codes)
    return codes
  }

  fun hasMfaEnabled(user: UserAccount): Boolean {
    return user.totpKey?.isNotEmpty() == true
  }

  fun validateTotpCode(user: UserAccount, code: String): Boolean {
    if (user.totpKey?.isNotEmpty() != true) return true
    return validateTotpCode(user.totpKey!!, code)
  }

  fun validateTotpCode(key: ByteArray, code: String): Boolean {
    if (code.length != 6) return false

    return try {
      val parsedCode = code.toInt()
      val expectedCode = generateCode(key)
      parsedCode == expectedCode
    } catch (e: NumberFormatException) {
      false
    }
  }

  private fun generateCode(key: ByteArray): Number {
    return totpGenerator.generateOneTimePassword(SecretKeySpec(key, MfaConfiguration.OTP_ALGORITHM), Instant.now())
  }
}
