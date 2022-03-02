package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.rate-limits")
class RateLimitProperties {
  var enabled: Boolean = true
}
