package io.tolgee.ee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.ee")
class EeProperties(
  var licenseServer: String = "https://app.tolgee.io",
)
