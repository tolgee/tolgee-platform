package io.tolgee.configuration

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
class EmailConfig(
  private val properties: TolgeeProperties,
) {
  @Bean
  fun mailSender(): JavaMailSender {
    val mailSender = JavaMailSenderImpl()

    val mailConfiguration = properties.smtp
    mailSender.host = mailConfiguration.host
    mailSender.port = mailConfiguration.port
    mailSender.username = mailConfiguration.username
    mailSender.password = mailConfiguration.password

    val props = mailSender.javaMailProperties
    props["mail.transport.protocol"] = "smtp"
    props["mail.smtp.auth"] = mailConfiguration.auth.toString()
    props["mail.smtp.ssl.enable"] = mailConfiguration.sslEnabled.toString()
    props["mail.smtp.starttls.enable"] = mailConfiguration.tlsEnabled.toString()
    props["mail.smtp.starttls.required"] = mailConfiguration.tlsRequired.toString()

    return mailSender
  }
}
