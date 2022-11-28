package io.tolgee.ee

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "tolgee.ee")
@ConstructorBinding
class EeProperties(
  var licenseServer: String = "https://app.tolgee.io",
)
