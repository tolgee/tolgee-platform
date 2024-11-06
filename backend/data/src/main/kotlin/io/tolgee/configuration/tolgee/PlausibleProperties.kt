package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.plausible")
class PlausibleProperties {
  var domain: String? = null
  var url: String = "https://tolgee.io"
  var scriptUrl: String =
    "https://tolgee.io/js/script.hash.outbound-links.pageview-props.revenue.tagged-events.js"
}
