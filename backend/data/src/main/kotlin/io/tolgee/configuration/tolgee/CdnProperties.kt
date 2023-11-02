package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.cdn")
class CdnProperties {
  var publicUrlPrefix: String? = null
  var azure: CdnAzureProperties = CdnAzureProperties()
  var s3: CdnS3Properties = CdnS3Properties()
}
