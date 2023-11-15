package io.tolgee.model.cdn

import io.tolgee.component.cdn.cachePurging.AzureCdnCachePurgingFactory
import io.tolgee.component.cdn.cachePurging.CdnCachePurgingFactory
import kotlin.reflect.KClass

enum class CdnPurgingType(val factory: KClass<out CdnCachePurgingFactory>) {
  AZURE_FRONT_DOOR(AzureCdnCachePurgingFactory::class)
}
