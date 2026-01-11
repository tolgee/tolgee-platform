package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.post-hog")
class PostHogProperties {
  var apiKey: String? = null
  var host: String? = null
}
