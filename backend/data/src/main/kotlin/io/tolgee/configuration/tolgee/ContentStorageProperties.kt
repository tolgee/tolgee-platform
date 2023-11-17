package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery.storage")
class ContentStorageProperties {
  var azure: ContentStorageAzureProperties = ContentStorageAzureProperties()
  var s3: ContentStorageS3Properties = ContentStorageS3Properties()
}
