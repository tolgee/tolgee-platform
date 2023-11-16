package io.tolgee.component.cdn.cachePurging

import io.tolgee.model.cdn.CdnPurgingConfig
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.stereotype.Component

@Component
class CdnPurgingProvider(
  private val applicationContext: AbstractApplicationContext,
  private val configs: List<CdnPurgingConfig>,
) {
  val defaultPurging by lazy {
    getDefaultFactory()
  }

  private fun getDefaultFactory(): CdnCachePurging? {
    val purgings = configs.mapNotNull {
      if (!it.enabled) {
        return@mapNotNull null
      }
      applicationContext.getBean(it.cdnPurgingType.factory.java).create(it)
    }
    if (purgings.size > 1) {
      throw RuntimeException("Exactly one CDN purging must be set")
    }

    return purgings.firstOrNull()
  }
}
