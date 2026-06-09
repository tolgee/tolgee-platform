package io.tolgee.component.contentDelivery.cachePurging

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.configuration.tolgee.all
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.stereotype.Component

@Component
class ContentDeliveryCachePurgingProvider(
  private val applicationContext: AbstractApplicationContext,
  private val tolgeeProperties: TolgeeProperties,
) {
  private val configs get() = tolgeeProperties.contentDelivery.cachePurging.all()

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
