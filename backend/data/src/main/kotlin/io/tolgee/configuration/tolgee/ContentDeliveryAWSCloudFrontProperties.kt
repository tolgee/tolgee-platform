package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.contentDelivery.AWSCloudFrontConfig

@DocProperty(prefix = "tolgee.content-delivery.cache-purging.aws-cloudfront")
class ContentDeliveryAWSCloudFrontProperties : AWSCloudFrontConfig {
  override var accessKey: String? = null
  override var secretKey: String? = null
  override var distributionId: String? = null
  override var contentRoot: String? = null
}
