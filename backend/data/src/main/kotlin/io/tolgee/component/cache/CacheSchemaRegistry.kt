package io.tolgee.component.cache

import io.tolgee.constants.Caches
import org.springframework.stereotype.Component

/**
 * Registry of cache schema revisions for automatic cache clearing on upgrade.
 *
 * ## Purpose
 * When a cached object's class structure changes (fields added/removed/renamed),
 * old serialized entries in Redis become incompatible with the new code.
 * This causes deserialization errors like KryoBufferUnderflowException.
 *
 * This registry tracks schema revisions for each cache, allowing the application
 * to clear only affected caches when their schema has changed.
 *
 * ## How It Works
 * Each cache has a schema revision number (simple integer). On startup,
 * [CacheSchemaCleaner] compares each cache's current revision against the
 * last-seen revision stored in Redis/local file. If current > stored, clear that cache.
 *
 * ## Adding a New Schema Change
 * When you modify a class that gets cached (e.g., adding a field to a data class
 * stored in Redis), increment that cache's revision number:
 *
 * ```kotlin
 * Caches.RATE_LIMITS to 2,  // Was 1, bumped for new field
 * ```
 *
 * Or add a new entry if the cache wasn't tracked before:
 *
 * ```kotlin
 * Caches.USER_ACCOUNTS to 1,  // First tracked schema change
 * ```
 *
 * ### Important Notes
 * - Just increment the number - no need to know the release version
 * - Only add/increment for caches with actual schema changes
 * - A "breaking change" is anything causing deserialization to fail:
 *   adding fields, removing fields, changing types, renaming fields
 * - Clearing a cache is safe but may cause temporary performance impact
 * - When in doubt, bump the revision - it's better than deserialization crashes
 *
 * @see CacheSchemaCleaner
 * @see Caches
 */
@Component
class CacheSchemaRegistry {
  companion object {
    /**
     * Map of cache name to its current schema revision.
     *
     * Format: `Caches.CACHE_NAME to <revision_number>`
     *
     * When you make a breaking change to a cached class, increment its revision.
     * If the cache isn't listed yet, add it with revision 1.
     */
    private val SCHEMA_REVISIONS =
      mapOf(
        // Revision 1: Added strikeCount and lastStrikeAt fields to Bucket class
        Caches.RATE_LIMITS to 1,
      )
  }

  /**
   * Returns the current schema revisions for all tracked caches.
   */
  fun getSchemaRevisions(): Map<String, Int> = SCHEMA_REVISIONS

  /**
   * Returns caches that need clearing based on stored revisions.
   *
   * @param storedRevisions Map of cache name to last-seen revision (from storage)
   * @return List of cache names where current revision > stored revision
   */
  fun getCachesToClear(storedRevisions: Map<String, Int>): List<String> {
    return SCHEMA_REVISIONS
      .filter { (cache, currentRevision) ->
        val storedRevision = storedRevisions[cache] ?: 0
        currentRevision > storedRevision
      }.keys
      .toList()
  }
}
