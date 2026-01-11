package io.tolgee.component.contentDelivery.cachePurging

import io.tolgee.model.contentDelivery.ContentDeliveryConfig

interface ContentDeliveryCachePurging {
  fun purgeForPaths(
    contentDeliveryConfig: ContentDeliveryConfig,
    paths: Set<String>,
  )
}
