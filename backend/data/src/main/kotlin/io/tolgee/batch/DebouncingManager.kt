package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Caches
import io.tolgee.util.Logging
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class DebouncingManager(
  private val currentDateProvider: CurrentDateProvider,
  private val batchJobService: BatchJobService,
  private val cacheManager: CacheManager
) : Logging {

  fun shouldNotBeDebounced(jobId: Long): Boolean {
    val job = batchJobService.getJobDto(jobId)
    val debounceDurationInMs = job.debounceDurationInMs ?: return true

    val firstAndLastCreatedAt = getFirstAndLastCreatedAt(job)

    val runAfter = debounceDurationInMs + firstAndLastCreatedAt.second
    if (runAfter > currentDateProvider.date.time) {
      return true
    }

    return isMaxWaitTimeExceeded(job)
  }

  private fun isMaxWaitTimeExceeded(job: BatchJobDto): Boolean {
    val firstCreatedAt = getFirstAndLastCreatedAt(job).first

    if (firstCreatedAt == -1L) {
      return false
    }

    return firstCreatedAt + (job.debounceMaxWaitTimeInMs ?: 0) < currentDateProvider.date.time
  }

  private fun getFirstAndLastCreatedAt(job: BatchJobDto): Pair<Long, Long> {
    val debouncingKey = job.debouncingKey ?: throw IllegalStateException("Job is not debounced")
    return getFirstAndLastCreatedAtFromCache(job.projectId, debouncingKey)
      ?: getFirstAndLastCreatedAtFromDbAndCache(job, debouncingKey)
  }

  private fun getFirstAndLastCreatedAtFromDbAndCache(job: BatchJobDto, debouncingKey: String): Pair<Long, Long> {
    val fromDb = batchJobService.getFirstAndLastCreatedAtByDebouncingKey(job.projectId, debouncingKey)
      ?: Pair(-1L, -1L)
    getCache()?.put(getCacheKey(job.projectId, debouncingKey), fromDb)
    return fromDb
  }

  fun getFirstAndLastCreatedAtFromCache(projectId: Long, debouncingKey: String): Pair<Long, Long>? {
    @Suppress("UNCHECKED_CAST")
    return getCache()?.get(getCacheKey(projectId, debouncingKey))?.get() as? Pair<Long, Long>?
  }

  private fun getCache(): Cache? = cacheManager.getCache(Caches.BATCH_JOBS)

  fun getCacheKey(projectId: Long, debouncingKey: String): List<Any> {
    return listOf("first-last-debounced-created-at", projectId, debouncingKey)
  }
}
