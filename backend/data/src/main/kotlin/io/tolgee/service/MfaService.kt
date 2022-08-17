package io.tolgee.service

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator
import io.tolgee.configuration.MfaConfiguration
import io.tolgee.model.UserAccount
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Service
class MfaService(
  private val userAccountService: UserAccountService,
  private val totpGenerator: TimeBasedOneTimePasswordGenerator
) {
  fun getCode(key: ByteArray): Number {
    return totpGenerator.generateOneTimePassword(SecretKeySpec(key, MfaConfiguration.OTP_ALGORITHM), Instant.now())
  }

  fun generateRecoveryKeys(): List<String> {
    return List(10) { UUID.randomUUID().toString() }
  }

  fun validateOtpCode(code: String, user: UserAccount): Boolean {
    if (user.totpKey?.isNotEmpty() != true) return true
    if (code.length == 6) {
      // Classic OTP code
      return validateOtpCode(code, user.totpKey!!)
    }

    // Recovery key
    if (user.totpRecoveryCodes.contains(code)) {
      userAccountService.consumeMfaRecoveryCode(user, code)
      return true
    }

    return false
  }

  fun validateOtpCode(code: String, key: ByteArray): Boolean {
    return try {
      val parsedCode = code.toInt()
      val expectedCode = getCode(key)
      parsedCode == expectedCode
    } catch (e: NumberFormatException) {
      false
    }
  }
}
