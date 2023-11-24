package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery")
class ContentDeliveryProperties {
  var publicUrlPrefix: String? = null
  var storage: ContentStorageProperties = ContentStorageProperties()
  @DocProperty(hidden = true)
  var cachePurging: ContentDeliveryCachePurgingProperties = ContentDeliveryCachePurgingProperties()
}
