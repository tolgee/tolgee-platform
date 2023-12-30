package io.tolgee.component.contentDelivery.cachePurging

import io.tolgee.model.contentDelivery.AzureFrontDoorConfig
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class AzureContentDeliveryCachePurgingFactory(
  private val restTemplate: RestTemplate,
) : ContentDeliveryCachePurgingFactory {
  override fun create(config: Any): AzureContentDeliveryCachePurging {
    return AzureContentDeliveryCachePurging(config as AzureFrontDoorConfig, restTemplate, AzureCredentialProvider())
  }
}
