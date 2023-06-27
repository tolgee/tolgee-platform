package io.tolgee.batch

import io.tolgee.constants.Caches
import io.tolgee.model.batch.BatchJob
import io.tolgee.repository.BatchJobRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CachingBatchJobService(
  private val batchJobRepository: BatchJobRepository,
  @Lazy
  private val batchJobService: BatchJobService,
) {

  @Transactional
  @CacheEvict(
    cacheNames = [Caches.BATCH_JOBS],
    key = "#result.id"
  )
  fun saveJob(batchJob: BatchJob): BatchJob {
    return batchJobRepository.save(batchJob)
  }

  @Cacheable(
    cacheNames = [Caches.BATCH_JOBS],
    key = "#id"
  )
  fun findJobDto(id: Long): BatchJobDto? {
    val entity = batchJobService.findJobEntity(id) ?: return null
    return BatchJobDto.fromEntity(entity)
  }
}
