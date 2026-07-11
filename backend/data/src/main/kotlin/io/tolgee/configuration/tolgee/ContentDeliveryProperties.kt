package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery")
@DocProperty(
  displayName = "Content Delivery",
  description =
    "These properties are used to configure " +
      "default server content delivery storage." +
      "\n \n" +
      "To get content delivery working, " +
      "you have to configure the Storage (e.g. S3 or Blob).",
)
class ContentDeliveryProperties {
  @DocProperty(
    description =
      "The prefix URL where the content is accessible from the public. If this property is not null, " +
        "content delivery will be enabled on your server." +
        "\n\n" +
        "For example, if you set this property to `https://cdn.example.com/`",
  )
  var publicUrlPrefix: String? = null

  @DocProperty(description = "Configuration of the storage. You have to configure exactly one storage.")
  var storage: ContentStorageProperties = ContentStorageProperties()

  @DocProperty(
    displayName = "Cache purging",
    description =
      "Several services can be used as cache. Tolgee is able to purge the cache when " +
        "new files are published when this configuration is set.",
  )
  var cachePurging: ContentDeliveryCachePurgingProperties = ContentDeliveryCachePurgingProperties()
}
