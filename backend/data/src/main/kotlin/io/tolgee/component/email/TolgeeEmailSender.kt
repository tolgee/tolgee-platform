package io.tolgee.component.email

import io.tolgee.dtos.misc.EmailParams
import io.tolgee.email.EmailService
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class TolgeeEmailSender(
  private val emailService: EmailService,
) {
  fun sendEmail(params: EmailParams) {
    val properties = mapOf<String, Any>()
      .let { if (params.text != null) it.plus("content" to params.text!!) else it }
      .let { if (params.header != null) it.plus("header" to params.header!!) else it }
      .let { if (params.recipientName != null) it.plus("recipientName" to params.recipientName!!) else it }
    emailService.sendEmailTemplate(
      recipient = params.to,
      subject = params.subject,
      template = params.templateName ?: "default",
      locale = Locale.ENGLISH,
      properties = properties,
      attachments = params.attachments,
      bcc = params.bcc,
      replyTo = params.replyTo,
    )
  }
}
