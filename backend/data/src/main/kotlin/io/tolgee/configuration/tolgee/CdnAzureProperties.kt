package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.cdn.azure")
class CdnAzureProperties {
  var connectionString: String? = null
  var containerName: String? = null
}
