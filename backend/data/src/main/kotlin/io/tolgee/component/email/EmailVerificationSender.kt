package io.tolgee.component.email

import io.tolgee.dtos.misc.EmailParams
import io.tolgee.model.UserAccount
import org.springframework.stereotype.Component

@Component
class EmailVerificationSender(
  private val tolgeeEmailSender: TolgeeEmailSender,
) {
  fun sendEmailVerification(
    user: UserAccount,
    email: String,
    resultCallbackUrl: String?,
    code: String,
    isSignUp: Boolean = true,
  ) {
    val url = "$resultCallbackUrl/${user.id}/$code"
    val params =
      EmailParams(
        to = email,
        subject = "Tolgee e-mail verification",
        templateName = "registration-confirm",
        properties = mapOf("confirmUrl" to url, "username" to user.name, "isSignUp" to isSignUp),
      )
    tolgeeEmailSender.sendEmail(params)
  }
}
