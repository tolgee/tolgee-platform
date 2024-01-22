package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery")
@DocProperty(
  displayName = "Content Delivery",
  description =
    "These properties are used to configure " +
      "default server content delivery storage.",
)
class ContentDeliveryProperties {
  @DocProperty(
    description =
      "The root path where the content is accessible by the public. If this property is not null, " +
        "content delivery will be enabled on your server.",
  )
  var publicUrlPrefix: String? = null

  @DocProperty(description = "Configuration of the storage. You have to configure exactly one storage.")
  var storage: ContentStorageProperties = ContentStorageProperties()

  @DocProperty(hidden = true)
  var cachePurging: ContentDeliveryCachePurgingProperties = ContentDeliveryCachePurgingProperties()
}
