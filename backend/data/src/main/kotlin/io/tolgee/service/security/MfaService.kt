package io.tolgee.service.security

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator
import io.tolgee.api.isMfaEnabled
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.configuration.SecurityConfiguration
import io.tolgee.constants.Message
import io.tolgee.dtos.request.UserMfaRecoveryRequestDto
import io.tolgee.dtos.request.UserTotpDisableRequestDto
import io.tolgee.dtos.request.UserTotpEnableRequestDto
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
  private val currentDateProvider: CurrentDateProvider,
) {
  private val base32 = Base32()

  fun enableTotpFor(
    user: UserAccount,
    dto: UserTotpEnableRequestDto,
  ) {
    requireCredentials(user, dto.password)

    if (user.isMfaEnabled) {
      throw BadRequestException(Message.MFA_ENABLED)
    }

    val key = base32.decode(dto.totpKey)
    userAccountService.enableMfaTotp(user, key, dto.otp)
  }

  fun disableTotpFor(
    user: UserAccount,
    dto: UserTotpDisableRequestDto,
  ) {
    requireCredentials(user, dto.password)

    if (!hasMfaEnabled(user)) {
      throw BadRequestException(Message.MFA_NOT_ENABLED)
    }

    userAccountService.disableMfaTotp(user)
  }

  fun regenerateRecoveryCodes(
    user: UserAccount,
    dto: UserMfaRecoveryRequestDto,
  ): List<String> {
    requireCredentials(user, dto.password)

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

  private fun requireCredentials(
    user: UserAccount,
    password: String,
  ) {
    try {
      userCredentialsService.checkUserCredentials(user, password)
    } catch (e: AuthenticationException) {
      // Re-throw as a permission exception to set status to 403 instead of 401
      throw PermissionException(Message.BAD_CREDENTIALS)
    }
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

    if (otp.isNullOrEmpty()) {
      throw AuthenticationException(Message.MFA_ENABLED)
    }

    if (otp.length == totpGenerator.passwordLength) {
      userAccountService.consumeTotpCode(user, otp)
    } else {
      userAccountService.consumeMfaRecoveryCode(user, otp)
    }
  }

  /**
   * Stateless TOTP validator — returns `true` if [code] matches any of the codes in the
   * ±1 time-step window centred on the current time.
   *
   * **Do not use this for authentication.** Authentication flows must go through
   * [UserAccountService.consumeTotpCode], which additionally enforces replay prevention
   * against the user's stored [UserAccount.totpLastUsedTimeStep] (RFC 6238 §5.2).
   */
  fun validateTotpCode(
    key: ByteArray,
    code: String,
  ): Boolean = findMatchingTimeStep(key, code, minExclusiveTimeStep = null) != null

  /**
   * Returns the absolute TOTP time step counter whose generated OTP equals [code], or `null`
   * if no candidate in the ±1 window matches.
   *
   * @param minExclusiveTimeStep if non-null, candidates with counter `<=` this value are
   *        filtered out. Used by [UserAccountService.consumeTotpCode] to enforce replay
   *        prevention: a code whose counter is not strictly greater than the user's stored
   *        last-used counter cannot match.
   */
  internal fun findMatchingTimeStep(
    key: ByteArray,
    code: String,
    minExclusiveTimeStep: Long?,
  ): Long? {
    if (code.length != totpGenerator.passwordLength) return null
    val parsed = code.toIntOrNull() ?: return null
    val now = currentTimeStep()
    return listOf(-1L, 0L, 1L)
      .map { now + it }
      .filter { minExclusiveTimeStep == null || it > minExclusiveTimeStep }
      .firstOrNull { step -> parsed == generateCodeForTimeStep(key, step) }
  }

  /**
   * Current absolute TOTP time step counter, derived from the mockable [currentDateProvider]
   * using the same millis-based quantization that the underlying [totpGenerator] uses
   * internally (`floor(epochMillis / timeStepMillis)`). Exposed as public so tests in other
   * Gradle modules can assert the exact seeded / burned counter value.
   */
  fun currentTimeStep(): Long = currentDateProvider.date.time / totpGenerator.timeStep.toMillis()

  private fun generateCodeForTimeStep(
    key: ByteArray,
    timeStep: Long,
  ): Int {
    val instant = Instant.ofEpochMilli(timeStep * totpGenerator.timeStep.toMillis())
    return totpGenerator.generateOneTimePassword(
      SecretKeySpec(key, SecurityConfiguration.OTP_ALGORITHM),
      instant,
    )
  }

  fun generateCode(
    key: ByteArray,
    timeSlotOffset: Long = 0,
  ): Int {
    return generateCodeForTimeStep(key, currentTimeStep() + timeSlotOffset)
  }

  fun generateStringCode(
    key: ByteArray,
    timeSlotOffset: Long = 0,
  ): String {
    return generateCode(key, timeSlotOffset)
      .toString()
      .padStart(totpGenerator.passwordLength, '0')
  }

  companion object {
    fun mfaEnabled(totpKeyOrNull: ByteArray?): Boolean {
      return totpKeyOrNull?.isNotEmpty() == true
    }
  }
}
