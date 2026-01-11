package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.smtp")
@DocProperty(
  description =
    "Configuration of SMTP server used to send emails to your users " +
      "like password reset links or notifications.\n" +
      "\n" +
      "For AWS SES it would look like this:\n" +
      "```\n" +
      "tolgee.smtp.host=email-smtp.eu-central-1.amazonaws.com\n" +
      "tolgee.smtp.username=*****************\n" +
      "tolgee.smtp.password=*****************\n" +
      "tolgee.smtp.port=465\n" +
      "tolgee.smtp.auth=true\n" +
      "tolgee.smtp.ssl-enabled=true\n" +
      "tolgee.smtp.from=Tolgee <no-reply@tolgee.yourserver.something>\n" +
      "```",
  displayName = "SMTP",
)
class SmtpProperties {
  @DocProperty(description = "SMTP server host")
  var host: String? = null

  @DocProperty(description = "The username for SMTP authentication")
  var username: String? = null

  @DocProperty(description = "Password for SMTP authentication")
  var password: String? = null

  @DocProperty(description = "SMTP server port")
  var port = 25

  @DocProperty(description = "Whether authentication is enabled.")
  var auth: Boolean = false

  @DocProperty(description = "Whether TLS is enabled.")
  var tlsEnabled: Boolean = false

  @DocProperty(description = "Whether SSL is enabled.")
  var sslEnabled: Boolean = false

  @DocProperty(description = "Whether TLS is required.")
  var tlsRequired: Boolean = false

  @DocProperty(description = "The sender name and address in standard SMTP format.")
  var from: String? = null
}
