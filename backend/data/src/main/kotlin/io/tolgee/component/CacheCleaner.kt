package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Clears all caches on startup when configured via [TolgeeProperties.cache.cleanOnStartup].
 *
 * This runs with @Order(100) to ensure [CacheSchemaCleaner] runs first (@Order(50)).
 * The schema cleaner handles revision-based cache clearing, while this component
 * provides a way to force-clear all caches if needed.
 */
@Component
class CacheCleaner(
  private val allCachesProvider: AllCachesProvider,
  private val cacheManager: CacheManager,
  private val tolgeeProperties: TolgeeProperties,
) {
  @EventListener
  @Order(100)
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
