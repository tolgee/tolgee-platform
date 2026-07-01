package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(prefix = "tolgee.post-hog")
class PostHogProperties {
  var apiKey: String? = null
  var host: String? = null
}
