package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class CacheCleaner(
  private val allCachesProvider: AllCachesProvider,
  private val cacheManager: CacheManager,
  private val tolgeeProperties: TolgeeProperties,
) {
  @EventListener
  fun onAppStartup(event: ApplicationReadyEvent) {
    if (tolgeeProperties.cache.cleanOnStartup) {
      cleanCaches()
    }
  }

  fun cleanCaches() {
    allCachesProvider.getAllCaches().forEach {
      cacheManager.getCache(it)?.clear()
    }
  }
}
