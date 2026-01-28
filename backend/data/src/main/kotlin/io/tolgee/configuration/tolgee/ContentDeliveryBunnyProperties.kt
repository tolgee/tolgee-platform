package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.contentDelivery.ContentDeliveryCachePurgingType
import io.tolgee.model.contentDelivery.ContentDeliveryPurgingConfig

@DocProperty(prefix = "tolgee.content-delivery.cache-purging.bunny")
class ContentDeliveryBunnyProperties(
  var apiKey: String? = null,
  var urlPrefix: String? = null,
) : ContentDeliveryPurgingConfig {
  override val enabled: Boolean
    get() = !apiKey.isNullOrEmpty() && !urlPrefix.isNullOrEmpty()

  override val contentDeliveryCachePurgingType: ContentDeliveryCachePurgingType
    get() = ContentDeliveryCachePurgingType.BUNNY
}
