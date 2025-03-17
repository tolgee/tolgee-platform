package io.tolgee.configuration.tolgee

import io.tolgee.model.contentDelivery.AWSCloudFrontConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery.cache-purging.aws-cloudfront")
class ContentDeliveryAWSCloudFrontProperties : AWSCloudFrontConfig {
  override var accessKey: String? = null
  override var secretKey: String? = null
  override var distributionId: String? = null
  override var contentRoot: String? = null
}
