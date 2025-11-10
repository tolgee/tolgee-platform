package io.tolgee.component.contentDelivery.cachePurging.awsCloudFront

import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingFactory
import io.tolgee.model.contentDelivery.AWSCloudFrontConfig
import org.springframework.stereotype.Component

@Component
class AWSCloudFrontContentDeliveryCachePurgingFactory : ContentDeliveryCachePurgingFactory {
  override fun create(config: Any): AWSCloudFrontContentDeliveryCachePurging {
    return AWSCloudFrontContentDeliveryCachePurging(config as AWSCloudFrontConfig, AWSCredentialProvider())
  }
}
