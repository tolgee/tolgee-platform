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
        header = "Verify your e-mail",
        text =
          """
          ${if (isSignUp) "Welcome to Tolgee. Thanks for signing up. \uD83C\uDF89<br/><br/>" else ""}
          
          To verify your e-mail, <b>follow this link</b>:<br/>
          <a href="$url">$url</a><br/><br/>
          """.trimIndent(),
      )
    tolgeeEmailSender.sendEmail(params)
  }
}
