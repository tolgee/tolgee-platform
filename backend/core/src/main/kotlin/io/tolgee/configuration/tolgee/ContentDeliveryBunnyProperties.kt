package io.tolgee.configuration.tolgee

import io.tolgee.model.contentDelivery.ContentDeliveryCachePurgingType
import io.tolgee.model.contentDelivery.ContentDeliveryPurgingConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery.cache-purging.bunny")
class ContentDeliveryBunnyProperties(
  var apiKey: String? = null,
  var urlPrefix: String? = null,
) : ContentDeliveryPurgingConfig {
  override val enabled: Boolean
    get() = !apiKey.isNullOrEmpty() && !urlPrefix.isNullOrEmpty()

  override val contentDeliveryCachePurgingType: ContentDeliveryCachePurgingType
    get() = ContentDeliveryCachePurgingType.BUNNY
}
