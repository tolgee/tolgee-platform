package io.tolgee.component.contentDelivery.cachePurging.awsCloudFront

import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingFactory
import io.tolgee.model.contentDelivery.AWSCloudFrontConfig
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class AWSCloudFrontContentDeliveryCachePurgingFactory(
  private val restTemplate: RestTemplate,
) : ContentDeliveryCachePurgingFactory {
  override fun create(config: Any): AWSCloudFrontContentDeliveryCachePurging {
    return AWSCloudFrontContentDeliveryCachePurging(config as AWSCloudFrontConfig, AWSCredentialProvider())
  }
}
