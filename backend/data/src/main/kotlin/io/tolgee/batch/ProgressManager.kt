package io.tolgee.batch

import io.tolgee.component.AtomicLongProvider
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.util.Logging
import io.tolgee.util.TolgeeAtomicLong
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.math.BigInteger
import javax.persistence.EntityManager

@Component
class ProgressManager(
  private val atomicLongProvider: AtomicLongProvider,
  private val entityManager: EntityManager,
  private val eventPublisher: ApplicationEventPublisher,
  private val transactionManager: PlatformTransactionManager
) : Logging {
  private fun getProgressAtomicLong(jobId: Long, currentExecutionId: Long? = null): TolgeeAtomicLong {
    return atomicLongProvider.get("batch_job_progress_$jobId") {
      getInitialProgress(jobId, currentExecutionId)
    }
  }

  private fun getCompletedChunksLong(jobId: Long): TolgeeAtomicLong {
    return atomicLongProvider.get("batch_job_completed_chunks_$jobId") {
      getInitialCompletedChunks(jobId)
    }
  }

  private fun getInitialCompletedChunks(jobId: Long): Long {
    return entityManager.createNativeQuery(
      """
            select count(distinct bjce1.id) 
            from batch_job_chunk_execution bjce1 
             left join batch_job_chunk_execution bjce2 
               on bjce1.batch_job_id = bjce2.batch_job_id 
                 and bjce1.chunk_number = bjce2.chunk_number
                 and bjce1.status = :failedStatus
             left join batch_job_chunk_execution bjce3
               on bjce1.batch_job_id = bjce3.batch_job_id 
                 and bjce1.chunk_number = bjce3.chunk_number
                 and bjce3.status = :pendingStatus
            where bjce1.batch_job_id = :jobId and (bjce1.status = :successStats and bjce3.id is null)
            """
    )
      .setParameter("jobId", jobId)
      .setParameter("failedStatus", BatchJobChunkExecutionStatus.FAILED.name)
      .setParameter("successStats", BatchJobChunkExecutionStatus.SUCCESS.name)
      .setParameter("pendingStatus", BatchJobChunkExecutionStatus.PENDING.name)
      .singleResult?.let { (it as BigInteger).toLong() - 1 } ?: 0
  }

  private fun getInitialProgress(jobId: Long, currentExecutionId: Long?): Long {
    return entityManager.createNativeQuery(
      """
            select sum(jsonb_array_length(success_targets)) 
            from batch_job_chunk_execution 
            where batch_job_id = :jobId and (id <> :currentId or :currentId is null)
            """
    )
      .setParameter("jobId", jobId)
      .setParameter("currentId", currentExecutionId)
      .singleResult?.let { (it as BigInteger).toLong() } ?: 0
  }

  fun handleProgress(execution: BatchJobChunkExecution) {
    val job = execution.batchJob
    val progress = getProgressAtomicLong(job.id, execution.id).addAndGet(execution.successTargets.size.toLong())
    if (execution.successTargets.isNotEmpty()) {
      eventPublisher.publishEvent(OnBatchOperationProgress(job, progress, job.totalItems.toLong()))
    }
    var completedChunks: Long? = null
    if (!execution.retry) {
      completedChunks = getCompletedChunksLong(job.id).addAndGet(1)
    }
    handleJobStatus(execution.batchJob, progress = progress, completedChunks = completedChunks)
  }

  fun handleJobStatus(job: BatchJob, completedChunks: Long? = null, progress: Long? = null) {
    val completedChunksNotNull = completedChunks ?: getCompletedChunksLong(job.id).get()

    logger.debug("Job ${job.id} completed chunks: $completedChunksNotNull of ${job.totalChunks}")
    if (job.totalChunks.toLong() != completedChunksNotNull) {
      return
    }

    val progressNotNull = progress ?: getProgressAtomicLong(job.id).get()
    logger.debug("Job ${job.id} progress: $progressNotNull of ${job.totalItems}")

    if (job.totalItems.toLong() != progressNotNull) {
      job.status = BatchJobStatus.FAILED
      entityManager.persist(job)
      eventPublisher.publishEvent(OnBatchOperationFailed(job))
      return
    }

    job.status = BatchJobStatus.SUCCESS
    logger.debug("Publishing success event for job ${job.id}")
    eventPublisher.publishEvent(OnBatchOperationSucceeded(job))
    entityManager.persist(job)
  }

  @Scheduled(fixedRate = 60 * 1000)
  fun updateProgress() {
    executeInNewTransaction(transactionManager) {
      val jobs = entityManager.createQuery(
        """
            select bj from BatchJob bj
            where bj.status = :pendingStatus or bj.status = :runningStatus
            """,
        BatchJob::class.java
      ).setParameter("pendingStatus", BatchJobStatus.PENDING)
        .setParameter("runningStatus", BatchJobStatus.RUNNING)
        .resultList

      jobs.forEach { job ->
        handleJobStatus(job)
      }
    }
  }
}
