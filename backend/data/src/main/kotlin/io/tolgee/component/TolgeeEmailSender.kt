package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.misc.TolgeeEmailParams
import io.tolgee.model.Invitation
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class TolgeeEmailSender(
  private val tolgeeProperties: TolgeeProperties,
  private val mailSender: JavaMailSender,
  private val frontendUrlProvider: FrontendUrlProvider
) {
  fun sendEmailVerification(
    userId: Long,
    email: String,
    resultCallbackUrl: String?,
    code: String,
    isSignUp: Boolean = true
  ) {
    val url = "$resultCallbackUrl/$userId/$code"
    val params = TolgeeEmailParams(
      to = email,
      subject = "Tolgee e-mail verification",
      text = """
        Hello! 👋<br/><br/>
        ${if (isSignUp) "Welcome to Tolgee. Thanks for signing up. \uD83C\uDF89<br/><br/>" else ""}
        
        To verify your e-mail, <b>follow this link</b>:<br/>
        <a href="$url">$url</a><br/><br/>
        
        Regards,<br/>
        Tolgee<br/><br/>
      """.trimIndent()
    )
    sendEmail(params)
  }

  fun sendInvitation(
    invitation: Invitation,
  ) {
    val email = invitation.email
    if (email.isNullOrBlank()) {
      return
    }
    val url = getInvitationAcceptUrl(invitation.code)
    val params = TolgeeEmailParams(
      to = email,
      subject = "Invitation to Tolgee",
      text = """
        Hello! 👋<br/><br/>
        Good news. ${getInvitationSentence(invitation)}<br/><br/>
        
        To accept the invitation, <b>follow this link</b>:<br/>
        <a href="$url">$url</a><br/><br/>
        
        Regards,<br/>
        Tolgee<br/><br/>
      """.trimIndent()
    )
    sendEmail(params)
  }

  private fun getInvitationSentence(invitation: Invitation): Any {
    val name = invitation.permission?.project?.name ?: invitation.organizationRole?.organization?.name
      ?: throw IllegalStateException("No organization or project!")

    return "You have been invited to project $name on Tolgee."
  }

  private fun getInvitationAcceptUrl(code: String): String {
    return "${frontendUrlProvider.frontEndUrl}/accept_invitation/$code"
  }

  private fun sendEmail(params: TolgeeEmailParams) {
    validateProps()
    val message = mailSender.createMimeMessage()
    val helper = MimeMessageHelper(
      message,
      MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
      StandardCharsets.UTF_8.name()
    )
    helper.setFrom(tolgeeProperties.smtp.from!!)
    helper.setTo(params.to)
    helper.setSubject(params.subject)
    val content = """
      <html>
      <body style="font-size: 15px">
      ${params.text}
      <img style="max-width: 100%; width:120px" src="cid:logo.png" />
      </body>
      </html>
    """.trimIndent()
    helper.setText(content, true)

    helper.addInline(
      "logo.png",
      { ClassPathResource("tolgee-logo.png").inputStream },
      "image/png"
    )

    mailSender.send(message)
  }

  private fun validateProps() {
    if (tolgeeProperties.smtp.from.isNullOrEmpty()) {
      throw IllegalStateException(
        """tolgee.smtp.from property not provided.
        |You have to configure smtp properties to send an e-mail.""".trimMargin()
      )
    }
  }
}
