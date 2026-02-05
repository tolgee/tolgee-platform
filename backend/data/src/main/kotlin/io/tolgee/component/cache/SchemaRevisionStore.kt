package io.tolgee.component.cache

/**
 * Interface for storing and retrieving cache schema revisions.
 *
 * This is used by [CacheSchemaCleaner] to track which schema revisions
 * have been seen, so caches are only cleared when their schema changes.
 *
 * Implementations can store revisions in different backends:
 * - Redis for distributed deployments
 * - Local file system for single-node deployments
 *
 * @see RedisSchemaRevisionStore
 * @see LocalSchemaRevisionStore
 */
interface SchemaRevisionStore {
  /**
   * Gets the stored schema revisions for all caches.
   *
   * @return Map of cache name to last-seen revision, empty map if no revisions stored yet
   */
  fun getStoredRevisions(): Map<String, Int>

  /**
   * Stores the current schema revisions.
   *
   * @param revisions Map of cache name to revision number
   */
  fun storeRevisions(revisions: Map<String, Int>)
}
