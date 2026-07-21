package io.tolgee.component.cache

import io.tolgee.component.machineTranslation.TranslateResult
import io.tolgee.component.reporting.ThrottledEventInCache
import io.tolgee.constants.Caches
import io.tolgee.security.ratelimit.Bucket
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class DataDirectAccessCacheTypeProvider : DirectAccessCacheTypeProvider {
  override fun getDirectAccessCacheTypes(): Map<String, KClass<*>> =
    mapOf(
      Caches.RATE_LIMITS to Bucket::class,
      Caches.MACHINE_TRANSLATIONS to TranslateResult::class,
      Caches.BUSINESS_EVENT_THROTTLING to ThrottledEventInCache::class,
    )
}
