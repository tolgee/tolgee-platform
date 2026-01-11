package io.tolgee.model.contentDelivery

interface AWSCloudFrontConfig : ContentDeliveryPurgingConfig {
  val accessKey: String?
  val secretKey: String?
  val distributionId: String?
  val contentRoot: String?

  override val contentDeliveryCachePurgingType: ContentDeliveryCachePurgingType
    get() = ContentDeliveryCachePurgingType.AWS_CLOUD_FRONT

  override val enabled: Boolean
    get() =
      arrayOf(accessKey, secretKey, distributionId)
        .all { it != null }
}
