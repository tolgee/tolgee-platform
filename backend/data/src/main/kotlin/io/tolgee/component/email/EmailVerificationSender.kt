package io.tolgee.component.email

import io.tolgee.dtos.misc.EmailParams
import org.springframework.stereotype.Component

@Component
class EmailVerificationSender(
  private val tolgeeEmailSender: TolgeeEmailSender,
) {
  fun sendEmailVerification(
    userId: Long,
    email: String,
    resultCallbackUrl: String?,
    code: String,
    isSignUp: Boolean = true,
  ) {
    val url = "$resultCallbackUrl/$userId/$code"
    val params =
      EmailParams(
        to = email,
        subject = "Tolgee e-mail verification",
        text =
          """
          Hello! 👋<br/><br/>
          ${if (isSignUp) "Welcome to Tolgee. Thanks for signing up. \uD83C\uDF89<br/><br/>" else ""}
          
          To verify your e-mail, <b>follow this link</b>:<br/>
          <a href="$url">$url</a><br/><br/>
          
          Regards,<br/>
          Tolgee
          """.trimIndent(),
      )
    tolgeeEmailSender.sendEmail(params)
  }
}
