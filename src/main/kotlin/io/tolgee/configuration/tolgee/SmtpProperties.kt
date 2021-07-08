package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.smtp")
class SmtpProperties {
  var host: String? = null

  var username: String? = null

  var password: String? = null

  var port = 25

  var auth: Boolean = false

  var tlsEnabled: Boolean = false

  var sslEnabled: Boolean = false

  var tlsRequired: Boolean = false

  var from: String? = null
}
