package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Component

@Component
class MailSender(properties: TolgeeProperties) : JavaMailSenderImpl() {
  init {
    val mailConfiguration = properties.smtp

    this.host = mailConfiguration.host
    this.port = mailConfiguration.port
    this.username = mailConfiguration.username
    this.password = mailConfiguration.password
    val props = this.javaMailProperties
    props["mail.transport.protocol"] = "smtp"
    props["mail.smtp.auth"] = mailConfiguration.auth.toString()
    props["mail.smtp.ssl.enable"] = mailConfiguration.sslEnabled.toString()
    props["mail.smtp.starttls.enable"] = mailConfiguration.tlsEnabled.toString()
    props["mail.smtp.starttls.required"] = mailConfiguration.tlsRequired.toString()
  }

  override fun send(simpleMessage: SimpleMailMessage) {
    super.send(simpleMessage)
  }
}
