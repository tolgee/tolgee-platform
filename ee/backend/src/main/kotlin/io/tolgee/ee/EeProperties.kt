package io.tolgee.ee

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "tolgee.ee")
@ConstructorBinding
class EeProperties(
  var licenseServer: String = "https://app.tolgee.io",
  val setPath: String = "/v2/public/licensing/set-key",
  val prepareSetKeyPath: String = "/v2/public/licensing/prepare-set-key",
  val reportUsagePath: String = "/v2/public/licensing/report-usage",
)
