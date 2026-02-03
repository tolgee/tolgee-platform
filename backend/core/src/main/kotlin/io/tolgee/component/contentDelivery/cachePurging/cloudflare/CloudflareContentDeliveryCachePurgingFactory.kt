package io.tolgee.component.contentDelivery.cachePurging.cloudflare

import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingFactory
import io.tolgee.configuration.tolgee.ContentDeliveryCloudflareProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class CloudflareContentDeliveryCachePurgingFactory(
  private val restTemplate: RestTemplate,
) : ContentDeliveryCachePurgingFactory {
  override fun create(config: Any): CloudflareContentDeliveryCachePurging {
    return CloudflareContentDeliveryCachePurging(
      config as ContentDeliveryCloudflareProperties,
      restTemplate,
    )
  }
}
