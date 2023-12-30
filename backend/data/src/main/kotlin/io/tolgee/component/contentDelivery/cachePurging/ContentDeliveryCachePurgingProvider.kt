package io.tolgee.component.contentDelivery.cachePurging

import io.tolgee.model.contentDelivery.ContentDeliveryPurgingConfig
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.stereotype.Component

@Component
class ContentDeliveryCachePurgingProvider(
  private val applicationContext: AbstractApplicationContext,
  private val configs: List<ContentDeliveryPurgingConfig>,
) {
  val defaultPurging by lazy {
    getDefaultFactory()
  }

  private fun getDefaultFactory(): ContentDeliveryCachePurging? {
    val purgings =
      configs.mapNotNull {
        if (!it.enabled) {
          return@mapNotNull null
        }
        applicationContext.getBean(it.contentDeliveryCachePurgingType.factory.java).create(it)
      }
    if (purgings.size > 1) {
      throw RuntimeException("Exactly one content delivery purging must be set")
    }

    return purgings.firstOrNull()
  }
}
