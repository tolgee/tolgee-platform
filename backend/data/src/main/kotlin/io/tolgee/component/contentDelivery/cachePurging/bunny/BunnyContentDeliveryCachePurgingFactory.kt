package io.tolgee.component.contentDelivery.cachePurging.bunny

import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingFactory
import io.tolgee.configuration.tolgee.ContentDeliveryBunnyProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class BunnyContentDeliveryCachePurgingFactory(
  private val restTemplate: RestTemplate,
) : ContentDeliveryCachePurgingFactory {
  override fun create(config: Any): BunnyContentDeliveryCachePurging {
    return BunnyContentDeliveryCachePurging(
      config as ContentDeliveryBunnyProperties,
      restTemplate,
    )
  }
}
