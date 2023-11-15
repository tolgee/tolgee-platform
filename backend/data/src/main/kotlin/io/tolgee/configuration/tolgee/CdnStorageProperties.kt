package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.cdn.storage")
class CdnStorageProperties {
  var azure: CdnAzureProperties = CdnAzureProperties()
  var s3: CdnS3Properties = CdnS3Properties()
}
