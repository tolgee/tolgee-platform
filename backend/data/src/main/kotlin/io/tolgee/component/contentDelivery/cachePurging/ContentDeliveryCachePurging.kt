package io.tolgee.component.contentDelivery.cachePurging

interface ContentDeliveryCachePurging {
  fun purgeForPaths(paths: Set<String>)
}
