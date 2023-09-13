package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.cdn")
class CdnProperties {
  var azure: CdnAzureProperties = CdnAzureProperties()
}
