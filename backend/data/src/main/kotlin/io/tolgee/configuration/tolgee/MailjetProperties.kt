package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.mailjet")
class MailjetProperties {
  var apiKey: String? = null
  var secretKey: String? = null
  var contactListId: Long? = null
}
