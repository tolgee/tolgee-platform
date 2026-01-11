package io.tolgee.model.contentDelivery

interface ContentDeliveryPurgingConfig {
  val enabled: Boolean

  val contentDeliveryCachePurgingType: ContentDeliveryCachePurgingType
}
