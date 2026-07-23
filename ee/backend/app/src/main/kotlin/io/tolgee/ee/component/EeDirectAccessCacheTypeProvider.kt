package io.tolgee.ee.component

import io.tolgee.component.cache.DirectAccessCacheTypeProvider
import io.tolgee.constants.Caches
import io.tolgee.ee.service.LlmProviderService
import org.springframework.stereotype.Component
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Component
class EeDirectAccessCacheTypeProvider : DirectAccessCacheTypeProvider {
  override fun getDirectAccessCacheTypes(): Map<String, KType> =
    mapOf(
      Caches.LLM_PROVIDERS to typeOf<LlmProviderService.Companion.ProviderInfo>(),
    )
}
