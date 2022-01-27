package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.send-in-blue")
class SendInBlueProperties {
  var apiKey: String? = null
  var listId: Long? = null
}
