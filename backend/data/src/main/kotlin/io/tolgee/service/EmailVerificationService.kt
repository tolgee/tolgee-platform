/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.component.email.EmailVerificationSender
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.events.user.OnUserEmailVerifiedFirst
import io.tolgee.events.user.OnUserUpdated
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.EmailVerification
import io.tolgee.model.UserAccount
import io.tolgee.repository.EmailVerificationRepository
import io.tolgee.security.ratelimit.RateLimitService
import io.tolgee.service.security.UserAccountService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
class EmailVerificationService(
  private val tolgeeProperties: TolgeeProperties,
  private val emailVerificationRepository: EmailVerificationRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val emailVerificationSender: EmailVerificationSender,
  private val rateLimitService: RateLimitService,
) {
  @Lazy
  @Autowired
  lateinit var userAccountService: UserAccountService

  @Transactional
  fun createForUser(
    userAccount: UserAccount,
    callbackUrl: String? = null,
    newEmail: String? = null,
  ): EmailVerification? {
    if (tolgeeProperties.authentication.needsEmailVerification) {
      val resultCallbackUrl = getCallbackUrl(callbackUrl)
      val code = generateCode()

      val emailVerification =
        userAccount.emailVerification?.also {
          it.newEmail = newEmail
          it.code = code
        } ?: EmailVerification(userAccount = userAccount, code = code, newEmail = newEmail)

      emailVerificationRepository.save(emailVerification)
      userAccount.emailVerification = emailVerification
      userAccountService.saveAndFlush(userAccount)

      if (newEmail != null) {
        emailVerificationSender.sendEmailVerification(userAccount, newEmail, resultCallbackUrl, code, false)
      } else {
        emailVerificationSender.sendEmailVerification(userAccount, userAccount.username, resultCallbackUrl, code)
      }
      return emailVerification
    }
    return null
  }

  @Transactional
  fun resendEmailVerification(
    userAccount: UserAccount,
    request: HttpServletRequest,
    callbackUrl: String? = null,
    newEmail: String? = null,
  ) {
    if (newEmail == null && isVerified(userAccount)) {
      throw BadRequestException(Message.EMAIL_ALREADY_VERIFIED)
    }

    val email = newEmail ?: getEmail(userAccount)
    val policy = rateLimitService.getEmailVerificationIpRateLimitPolicy(request, email)

    if (policy != null) {
      rateLimitService.consumeBucket(policy)
    }
    createForUser(userAccount, callbackUrl, newEmail)
  }

  fun getEmail(userAccount: UserAccount): String {
    return userAccount.emailVerification?.newEmail ?: userAccount.username
  }

  fun isVerified(userAccount: UserAccountDto): Boolean {
    return !tolgeeProperties.authentication.needsEmailVerification || userAccount.emailVerified
  }

  fun isVerified(userAccount: UserAccount): Boolean {
    return !(
      tolgeeProperties.authentication.needsEmailVerification &&
        userAccount.emailVerification != null
    )
  }

  fun check(userAccount: UserAccount) {
    if (!isVerified(userAccount)) {
      throw AuthenticationException(Message.EMAIL_NOT_VERIFIED)
    }
  }

  @Transactional
  fun verify(
    userId: Long,
    code: String,
  ) {
    val user = userAccountService.findActive(userId) ?: throw NotFoundException()
    val old = UserAccountDto.fromEntity(user)
    val emailVerification = user.emailVerification ?: throw BadRequestException(Message.EMAIL_ALREADY_VERIFIED)

    if (emailVerification.code != code) {
      throw BadRequestException(Message.EMAIL_VERIFICATION_CODE_NOT_VALID)
    }

    val newEmail = user.emailVerification?.newEmail
    setNewEmailIfChanged(newEmail, user)

    user.emailVerification = null
    emailVerificationRepository.delete(emailVerification)
    userAccountService.saveAndFlush(user)

    val isFirstEmailVerification = newEmail == null
    val isEmailChange = newEmail != null

    if (isFirstEmailVerification) {
      applicationEventPublisher.publishEvent(OnUserEmailVerifiedFirst(this, user))
    }

    if (isEmailChange) {
      applicationEventPublisher.publishEvent(OnUserUpdated(this, old, UserAccountDto.fromEntity(user)))
    }
  }

  private fun setNewEmailIfChanged(
    newEmail: String?,
    user: UserAccount,
  ) {
    newEmail?.let {
      user.username = newEmail
    }
  }

  private fun generateCode(): String {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val code = (1..50).map { charPool[Random.nextInt(0, charPool.size)] }.joinToString("")
    return code
  }

  private fun getCallbackUrl(callbackUrl: String?): String {
    var resultCallbackUrl = tolgeeProperties.frontEndUrl ?: callbackUrl

    if (resultCallbackUrl == null) {
      throw BadRequestException(Message.MISSING_CALLBACK_URL)
    }

    resultCallbackUrl += "/login/verify_email"
    return resultCallbackUrl
  }
}
