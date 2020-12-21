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
import kotlin.random.Random

@Service
open class EmailVerificationService(private val polygloatProperties: PolygloatProperties,
                                    private val userAccountRepository: UserAccountRepository,
                                    private val emailVerificationRepository: EmailVerificationRepository,
                                    private val mailSender: MailSender
) {
    fun createForUser(userAccount: UserAccount, callbackUrl: String? = null): EmailVerification? {
        if (polygloatProperties.authentication.needsEmailVerification) {
            val resultCallbackUrl = callbackUrl
                    ?: if (polygloatProperties.frontEndUrl != null)
                        polygloatProperties.frontEndUrl + "/login/verify_email"
                    else null

            if (resultCallbackUrl == null) {
                throw BadRequestException(Message.MISSING_CALLBACK_URL)
            }

            val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            val code = (1..50).map { charPool[Random.nextInt(0, charPool.size)] }.joinToString("")
            val emailVerification = EmailVerification(userAccount = userAccount, code = code)
            emailVerificationRepository.save(emailVerification)
            userAccount.emailVerification = emailVerification

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

            return emailVerification
        }
        return null
    }

    fun check(userAccount: UserAccount) {
        if (
                polygloatProperties.authentication.needsEmailVerification &&
                userAccount.emailVerification != null
        ) {
            throw AuthenticationException(Message.EMAIL_NOT_VERIFIED)
        }
    }

    fun verify(userId: Long, code: String) {
        val user = userAccountRepository.findById(userId).orElseThrow { NotFoundException() }
        if (user.emailVerification == null || user.emailVerification?.code != code) {
            throw NotFoundException()
        }
        emailVerificationRepository.delete(user.emailVerification!!)
    }

}
