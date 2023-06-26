package io.tolgee.batch

import io.tolgee.constants.Caches
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.repository.BatchJobRepository
import io.tolgee.util.executeInNewTransaction
import org.hibernate.LockOptions
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.LockModeType

@Service
class CachingBatchJobService(
  private val batchJobRepository: BatchJobRepository,
  private val applicationContext: ApplicationContext,
  @Lazy
  private val batchJobService: BatchJobService,
  private val entityManager: EntityManager,
  private val transactionManager: PlatformTransactionManager,
  private val progressManager: ProgressManager
) {

  @Transactional
  @CacheEvict(
    cacheNames = [Caches.BATCH_JOBS],
    key = "#result.id"
  )
  fun saveJob(batchJob: BatchJob): BatchJob {
    return batchJobRepository.save(batchJob)
  }

  @Transactional
  @CacheEvict(
    cacheNames = [Caches.BATCH_JOBS],
    key = "#id",
    beforeInvocation = true
  )
  fun cancelJob(jobId: Long): Boolean {
    val statuses = listOf(BatchJobStatus.RUNNING, BatchJobStatus.PENDING)
    val executions = executeInNewTransaction(transactionManager = transactionManager) {
      val executions = entityManager.createQuery(
        """
        from BatchJobChunkExecution bjce
        where bjce.batchJob.id = :id
        and status = :status
      """,
        BatchJobChunkExecution::class.java
      )
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .setHint(
          "javax.persistence.lock.timeout",
          LockOptions.SKIP_LOCKED
        )
        .setParameter("status", BatchJobChunkExecutionStatus.PENDING)
        .resultList

      executions.forEach { execution ->
        execution.status = BatchJobChunkExecutionStatus.CANCELLED
        entityManager.persist(execution)
      }

      entityManager.createQuery(
        """update BatchJob set status = 'CANCELLED' 
       where id = :id 
       and status in (:statuses)"""
      )
        .setParameter("id", jobId)
        .setParameter("statuses", statuses)
        .executeUpdate()

      executions
    }

    executions.forEach { progressManager.handleProgress(it) }

    return executeInNewTransaction(transactionManager) {
      val job = batchJobService.getJobEntity(jobId)
      job.status == BatchJobStatus.CANCELLED
    }
  }

  @Cacheable(
    cacheNames = [Caches.BATCH_JOBS],
    key = "#id"
  )
  fun findJobDto(id: Long): BatchJobDto? {
    val entity = batchJobService.findJobEntity(id) ?: return null
    return BatchJobDto.fromEntity(entity)
  }

  @Suppress("UNCHECKED_CAST")
  fun <RequestType> getProcessor(type: BatchJobType): ChunkProcessor<RequestType> =
    applicationContext.getBean(type.processor.java) as ChunkProcessor<RequestType>
}
