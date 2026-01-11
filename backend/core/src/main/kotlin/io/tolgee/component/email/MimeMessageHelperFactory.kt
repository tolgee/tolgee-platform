package io.tolgee.component.email

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class MimeMessageHelperFactory(
  private val mailSender: JavaMailSender,
) {
  fun create(): MimeMessageHelper {
    val message = mailSender.createMimeMessage()
    return MimeMessageHelper(
      message,
      MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
      StandardCharsets.UTF_8.name(),
    )
  }
}
