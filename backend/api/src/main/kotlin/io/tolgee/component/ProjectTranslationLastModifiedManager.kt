package io.tolgee.component

import io.tolgee.constants.Caches
import io.tolgee.events.OnProjectActivityEvent
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ProjectTranslationLastModifiedManager(
  val currentDateProvider: CurrentDateProvider,
  val cacheManager: CacheManager,
) {
  fun getLastModified(projectId: Long): Long {
    return getCache()?.get(projectId)?.get() as? Long
      ?: let {
        val now = currentDateProvider.date.time
        getCache()?.put(projectId, now)
        now
      }
  }

  private fun getCache(): Cache? = cacheManager.getCache(Caches.PROJECT_TRANSLATIONS_MODIFIED)

  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    event.activityRevision.projectId?.let { projectId ->
      getCache()?.put(projectId, currentDateProvider.date.time)
    }
  }
}
