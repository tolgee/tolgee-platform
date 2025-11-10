package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.constants.Caches
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.repository.BatchJobRepository
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
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
  private val entityManager: EntityManager,
) : Logging {
  @Transactional
  @CacheEvict(
    cacheNames = [Caches.BATCH_JOBS],
    key = "#result.id",
  )
  fun saveJob(batchJob: BatchJob): BatchJob {
    logger.debug("Saving job ${batchJob.id}, status: ${batchJob.status}")
    return batchJobRepository.save(batchJob)
  }

  @Transactional
  @CacheEvict(
    cacheNames = [Caches.BATCH_JOBS],
    key = "#jobId",
  )
  fun setRunningState(jobId: Long) {
    logger.debug("Setting running state for job $jobId")
    entityManager
      .createQuery("""update BatchJob set status = :status where id = :id and status = :pendingStatus""")
      .setParameter("status", BatchJobStatus.RUNNING)
      .setParameter("id", jobId)
      .setParameter("pendingStatus", BatchJobStatus.PENDING)
      .executeUpdate()
  }

  @Cacheable(
    cacheNames = [Caches.BATCH_JOBS],
    key = "#id",
  )
  fun findJobDto(id: Long): BatchJobDto? {
    val entity = batchJobService.findJobEntity(id) ?: return null
    logger.debug("Getting batch job $id from database, status: ${entity.status}")
    return BatchJobDto.fromEntity(entity)
  }

  @CacheEvict(
    cacheNames = [Caches.BATCH_JOBS],
    key = "#jobId",
  )
  fun evictJobCache(jobId: Long) {
  }
}
