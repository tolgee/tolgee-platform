package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.sentry")
class SentryProperties {
    var enabled = false
    var serverDsn: String? = null
    var clientDsn: String? = null
}
