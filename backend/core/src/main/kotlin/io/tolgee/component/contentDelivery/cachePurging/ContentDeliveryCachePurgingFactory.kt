package io.tolgee.component.contentDelivery.cachePurging

interface ContentDeliveryCachePurgingFactory {
  fun create(config: Any): ContentDeliveryCachePurging
}
