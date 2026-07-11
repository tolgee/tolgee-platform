package io.tolgee.component

import io.tolgee.constants.Caches
import io.tolgee.events.OnProjectActivityEvent
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProjectTranslationLastModifiedManager(
  val currentDateProvider: CurrentDateProvider,
  val cacheManager: CacheManager,
) {
  /**
   * Returns the last modification information for a given project.
   * If no information exists in the cache, creates new modification info with the current timestamp and UUID,
   * stores it in the cache and returns it.
   *
   * @param projectId The ID of the project to get last modified info for
   * @return LastModifiedInfo containing timestamp and ETag for the project
   */
  fun getLastModifiedInfo(projectId: Long): LastModifiedInfo {
    return getCache()?.get(projectId)?.get() as? LastModifiedInfo
      ?: let {
        val now = getCurrentInfo()
        getCache()?.put(projectId, now)
        now
      }
  }

  private fun getCache(): Cache? = cacheManager.getCache(Caches.PROJECT_TRANSLATIONS_MODIFIED)

  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    event.activityRevision.projectId?.let { projectId ->
      getCache()?.put(projectId, getCurrentInfo())
    }
  }

  private fun getCurrentInfo(): LastModifiedInfo {
    return LastModifiedInfo(
      lastModified = currentDateProvider.date.time,
      eTag = UUID.randomUUID().toString(),
    )
  }

  data class LastModifiedInfo(
    val lastModified: Long,
    val eTag: String,
  )
}
