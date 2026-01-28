package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(prefix = "tolgee.plausible")
class PlausibleProperties {
  var domain: String? = null
  var url: String = "https://tolgee.io"
  var scriptUrl: String =
    "https://tolgee.io/js/script.hash.outbound-links.pageview-props.revenue.tagged-events.js"
}
