package io.tolgee.component.email

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.misc.EmailParams
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class TolgeeEmailSender(
  private val tolgeeProperties: TolgeeProperties,
  private val mailSender: JavaMailSender,
  private val mimeMessageHelperFactory: MimeMessageHelperFactory,
) {
  fun sendEmail(params: EmailParams) {
    validateProps()
    val helper = mimeMessageHelperFactory.create()
    helper.setFrom(params.from ?: tolgeeProperties.smtp.from!!)
    helper.setTo(params.to)
    params.replyTo?.let {
      helper.setReplyTo(it)
    }
    if (!params.bcc.isNullOrEmpty()) {
      helper.setBcc(params.bcc!!)
    }
    helper.setSubject(params.subject)
    val content =
      """
      <html>
      <body style="font-size: 15px">
      ${params.text}<br/><br/>
      <img style="max-width: 100%; width:120px" src="cid:logo.png" />
      </body>
      </html>
      """.trimIndent()
    helper.setText(content, true)

    params.attachments.forEach {
      helper.addAttachment(it.name, it.inputStreamSource)
    }

    helper.addInline(
      "logo.png",
      { ClassPathResource("tolgee-logo.png").inputStream },
      "image/png",
    )

    mailSender.send(helper.mimeMessage)
  }

  private fun validateProps() {
    if (tolgeeProperties.smtp.from.isNullOrEmpty()) {
      throw IllegalStateException(
        """tolgee.smtp.from property not provided.
        |You have to configure smtp properties to send an e-mail.
        """.trimMargin(),
      )
    }
  }

  fun getSignature(): String {
    return """
      <br /><br />
      Best regards,
      <br />
      Tolgee Team
      """.trimIndent()
  }
}
