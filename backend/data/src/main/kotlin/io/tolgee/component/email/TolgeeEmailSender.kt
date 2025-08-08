package io.tolgee.component.email

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.misc.EmailParams
import io.tolgee.email.EmailService
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class TolgeeEmailSender(
  private val tolgeeProperties: TolgeeProperties,
  private val emailService: EmailService,
) {
  fun sendEmail(params: EmailParams) {
    validateProps()
    emailService.sendEmailTemplate(
      recipient = params.to,
      subject = params.subject,
      template = "default",
      locale = Locale.ENGLISH
    )
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
