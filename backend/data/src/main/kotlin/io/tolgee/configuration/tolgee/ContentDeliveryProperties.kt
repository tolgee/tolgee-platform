package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery")
class ContentDeliveryProperties {
  var publicUrlPrefix: String? = null
  var storage: ContentStorageProperties = ContentStorageProperties()
  var cachePurging: ContentDeliveryCachePurgingProperties = ContentDeliveryCachePurgingProperties()
}
