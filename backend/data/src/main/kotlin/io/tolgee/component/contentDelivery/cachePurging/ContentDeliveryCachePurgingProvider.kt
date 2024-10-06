package io.tolgee.component.contentDelivery.cachePurging

import io.tolgee.model.contentDelivery.ContentDeliveryPurgingConfig
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.stereotype.Component

@Component
class ContentDeliveryCachePurgingProvider(
  private val applicationContext: AbstractApplicationContext,
  private val configs: List<ContentDeliveryPurgingConfig>,
) {
  val purgings by lazy {
    getDefaultFactory()
  }

  private fun getDefaultFactory(): List<ContentDeliveryCachePurging> {
    return configs.mapNotNull {
      if (!it.enabled) {
        return@mapNotNull null
      }
      applicationContext.getBean(it.contentDeliveryCachePurgingType.factory.java).create(it)
    }
  }
}
