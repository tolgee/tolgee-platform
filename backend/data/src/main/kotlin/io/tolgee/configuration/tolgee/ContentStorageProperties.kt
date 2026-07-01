package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.NestedConfigurationProperty

@DocProperty(prefix = "tolgee.content-delivery.storage")
class ContentStorageProperties {
  @DocProperty(description = "Configuration of Azure Blob storage")
  @NestedConfigurationProperty
  var azure: ContentStorageAzureProperties = ContentStorageAzureProperties()

  @DocProperty(description = "Configuration of S3 bucket")
  @NestedConfigurationProperty
  var s3: ContentStorageS3Properties = ContentStorageS3Properties()
}
