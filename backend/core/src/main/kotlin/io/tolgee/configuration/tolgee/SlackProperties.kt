package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.slack")
class SlackProperties {
  var token: String? = null
  var signingSecret: String? = null
  var clientId: String? = null
  var clientSecret: String? = null
}
