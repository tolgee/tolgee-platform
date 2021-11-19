package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.import")
class ImportProperties {
  var dir: String? = null
  var createImplicitApiKey: Boolean = false
}
