/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.service

import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.constants.Message
import io.polygloat.exceptions.AuthenticationException
import io.polygloat.exceptions.BadRequestException
import io.polygloat.exceptions.NotFoundException
import io.polygloat.model.EmailVerification
import io.polygloat.model.UserAccount
import io.polygloat.repository.EmailVerificationRepository
import io.polygloat.repository.UserAccountRepository
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
open class EmailVerificationService(private val polygloatProperties: PolygloatProperties,
                                    private val userAccountRepository: UserAccountRepository,
                                    private val emailVerificationRepository: EmailVerificationRepository,
                                    private val mailSender: MailSender
) {
    @Transactional
    open fun createForUser(userAccount: UserAccount, callbackUrl: String? = null): EmailVerification? {
        if (polygloatProperties.authentication.needsEmailVerification) {
            val resultCallbackUrl = getCallbackUrl(callbackUrl)

            val code = generateCode()
            val emailVerification = EmailVerification(userAccount = userAccount, code = code)

            emailVerificationRepository.save(emailVerification)
            userAccount.emailVerification = emailVerification

            sendMail(userAccount, resultCallbackUrl, code)

            return emailVerification
        }
        return null
    }

    open fun check(userAccount: UserAccount) {
        if (
                polygloatProperties.authentication.needsEmailVerification &&
                userAccount.emailVerification != null
        ) {
            throw AuthenticationException(Message.EMAIL_NOT_VERIFIED)
        }
    }

    open fun verify(userId: Long, code: String) {
        val user = userAccountRepository.findById(userId).orElseThrow { NotFoundException() }
        if (user.emailVerification == null || user.emailVerification?.code != code) {
            throw NotFoundException()
        }
        emailVerificationRepository.delete(user.emailVerification!!)
    }

    private fun sendMail(userAccount: UserAccount, resultCallbackUrl: String?, code: String) {
        val message = SimpleMailMessage()
        message.setTo(userAccount.username!!)
        message.subject = "Polygloat e-mail verification"
        val url = "$resultCallbackUrl/${userAccount.id}/$code"
        message.text = """
                    Hello!
                    Welcome to Polygloat!
                    
                    To verify your e-mail click on this link: 
                    $url
                    
                    Regards, 
                    Polygloat
                    """.trimIndent()
        message.from = polygloatProperties.smtp.from
        mailSender.send(message)
    }

    private fun generateCode(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val code = (1..50).map { charPool[Random.nextInt(0, charPool.size)] }.joinToString("")
        return code
    }

    private fun getCallbackUrl(callbackUrl: String?): String {
        var resultCallbackUrl = polygloatProperties.frontEndUrl ?: callbackUrl

        if (resultCallbackUrl == null) {
            throw BadRequestException(Message.MISSING_CALLBACK_URL)
        }

        resultCallbackUrl += "/login/verify_email"
        return resultCallbackUrl
    }
}
