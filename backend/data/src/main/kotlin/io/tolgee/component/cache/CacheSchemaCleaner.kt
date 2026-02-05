package io.tolgee.component.cache

import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Clears caches that have had schema changes since last startup.
 *
 * ## How It Works
 * On startup, this component:
 * 1. Gets current schema revisions from [CacheSchemaRegistry]
 * 2. Gets stored revisions from [SchemaRevisionStore]
 * 3. Compares them to find caches where current > stored
 * 4. Clears only those affected caches
 * 5. Updates stored revisions to current values
 *
 * ## Example
 * ```
 * Registry: { rateLimits: 2, userAccounts: 1 }
 * Stored:   { rateLimits: 1 }
 *
 * Result:
 *   - rateLimits: 2 > 1 → CLEAR
 *   - userAccounts: 1 > 0 (not stored) → CLEAR
 *
 * After: stored = { rateLimits: 2, userAccounts: 1 }
 * Next restart: no clearing needed (revisions match)
 * ```
 *
 * ## Edge Cases
 * - **First run / no stored revisions:** Clears all tracked caches (safe default)
 * - **New cache added to registry:** Cleared on first run (stored revision = 0)
 * - **Cache removed from registry:** Ignored (no harm, old revision stays in storage)
 *
 * ## Concurrency Note
 * In distributed deployments, multiple instances may race during startup and all
 * clear the same caches. This is harmless - clearing a cache twice has no ill effects,
 * just a minor performance impact while caches warm up.
 *
 * @see CacheSchemaRegistry
 * @see SchemaRevisionStore
 */
@Component
class CacheSchemaCleaner(
  private val schemaRegistry: CacheSchemaRegistry,
  private val revisionStore: SchemaRevisionStore,
  private val cacheManager: CacheManager,
) : Logging {
  /**
   * Runs on application startup, before the general [CacheCleaner].
   *
   * Uses @Order(50) to ensure it runs before CacheCleaner's @Order(100).
   */
  @EventListener
  @Order(50)
  fun onApplicationReady(event: ApplicationReadyEvent) {
    val currentRevisions = schemaRegistry.getSchemaRevisions()
    val storedRevisions = revisionStore.getStoredRevisions()

    val cachesToClear = schemaRegistry.getCachesToClear(storedRevisions)

    var allCachesCleared = true

    if (cachesToClear.isNotEmpty()) {
      logger.info(
        "Cache schema changes detected, clearing caches: {} (stored: {}, current: {})",
        cachesToClear,
        storedRevisions,
        currentRevisions,
      )
      val failedCaches = clearCaches(cachesToClear)
      if (failedCaches.isNotEmpty()) {
        allCachesCleared = false
        logger.warn(
          "Some caches failed to clear: {}. Will retry on next startup.",
          failedCaches,
        )
      }
    } else if (storedRevisions.isEmpty() && currentRevisions.isEmpty()) {
      logger.debug("No cache schemas tracked, nothing to clear")
    } else {
      logger.debug("Cache schema revisions unchanged, no clearing needed")
    }

    // Only update stored revisions if all caches cleared successfully
    // This ensures failed clears will be retried on next startup
    if (currentRevisions.isNotEmpty() && allCachesCleared) {
      revisionStore.storeRevisions(currentRevisions)
    }
  }

  /**
   * Clears the specified caches.
   *
   * @return Set of cache names that failed to clear
   */
  private fun clearCaches(cacheNames: Collection<String>): Set<String> {
    val failedCaches = mutableSetOf<String>()
    cacheNames.forEach { cacheName ->
      try {
        val cache = cacheManager.getCache(cacheName)
        if (cache == null) {
          failedCaches.add(cacheName)
          logger.warn(
            "Cache {} not found in CacheManager (check CacheSchemaRegistry and Caches.caches are in sync)",
            cacheName,
          )
          return@forEach
        }
        cache.clear()
        logger.debug("Cleared cache: {}", cacheName)
      } catch (e: Exception) {
        failedCaches.add(cacheName)
        logger.warn("Failed to clear cache {}", cacheName, e)
      }
    }
    return failedCaches
  }
}
