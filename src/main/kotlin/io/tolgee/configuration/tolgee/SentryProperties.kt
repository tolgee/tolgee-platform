package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.sentry")
class SentryProperties {
  var serverDsn: String? = null
  var clientDsn: String? = null
}
