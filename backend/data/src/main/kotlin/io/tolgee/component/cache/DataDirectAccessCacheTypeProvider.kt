package io.tolgee.component.cache

import io.tolgee.component.machineTranslation.TranslateResult
import io.tolgee.component.reporting.ThrottledEventInCache
import io.tolgee.constants.Caches
import io.tolgee.security.ratelimit.Bucket
import org.springframework.stereotype.Component
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Component
class DataDirectAccessCacheTypeProvider : DirectAccessCacheTypeProvider {
  override fun getDirectAccessCacheTypes(): Map<String, KType> =
    mapOf(
      Caches.RATE_LIMITS to typeOf<Bucket>(),
      Caches.MACHINE_TRANSLATIONS to typeOf<TranslateResult>(),
      Caches.BUSINESS_EVENT_THROTTLING to typeOf<ThrottledEventInCache>(),
    )
}
