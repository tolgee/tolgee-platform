/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.EmailVerification
import io.tolgee.model.UserAccount
import io.tolgee.repository.EmailVerificationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
class EmailVerificationService(
  private val tolgeeProperties: TolgeeProperties,
  private val emailVerificationRepository: EmailVerificationRepository,
  private val mailSender: MailSender,
) {
  @Autowired
  lateinit var userAccountService: UserAccountService

  @Transactional
  fun createForUser(
    userAccount: UserAccount,
    callbackUrl: String? = null,
    newEmail: String? = null
  ): EmailVerification? {
    if (tolgeeProperties.authentication.needsEmailVerification) {
      val resultCallbackUrl = getCallbackUrl(callbackUrl)
      val code = generateCode()

      val emailVerification = userAccount.emailVerification?.also {
        it.newEmail = newEmail
        it.code = code
      } ?: EmailVerification(userAccount = userAccount, code = code, newEmail = newEmail)

      emailVerificationRepository.save(emailVerification)
      userAccount.emailVerification = emailVerification

      if (newEmail != null) {
        sendMail(userAccount.id!!, newEmail, resultCallbackUrl, code, false)
      } else {
        sendMail(userAccount.id!!, userAccount.username!!, resultCallbackUrl, code)
      }

      return emailVerification
    }
    return null
  }

  fun check(userAccount: UserAccount) {
    if (
      tolgeeProperties.authentication.needsEmailVerification &&
      userAccount.emailVerification != null
    ) {
      throw AuthenticationException(Message.EMAIL_NOT_VERIFIED)
    }
  }

  fun verify(userId: Long, code: String) {
    val user = userAccountService.get(userId).orElseThrow { NotFoundException() }
    if (user!!.emailVerification == null || user.emailVerification?.code != code) {
      throw NotFoundException()
    }

    user.emailVerification?.newEmail?.let {
      user.username = user.emailVerification?.newEmail!!
    }

    userAccountService.save(user)
    emailVerificationRepository.delete(user.emailVerification!!)
  }

  private fun sendMail(
    userId: Long,
    email: String,
    resultCallbackUrl: String?,
    code: String,
    isSignUp: Boolean = true
  ) {
    val message = SimpleMailMessage()
    message.setTo(email)
    message.subject = "Tolgee e-mail verification"
    val url = "$resultCallbackUrl/$userId/$code"
    message.text = """
                    Hello!
                    ${if (isSignUp) "Welcome to Tolgee!" else ""}
                    
                    To verify your e-mail click on this link: 
                    $url
                    
                    Regards,
                    Tolgee
    """.trimIndent()
    message.from = tolgeeProperties.smtp.from
    mailSender.send(message)
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
