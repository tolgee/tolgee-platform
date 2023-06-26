package io.tolgee.batch

import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import javax.persistence.EntityManager

@Component
class ProgressManager(
  private val entityManager: EntityManager,
  private val eventPublisher: ApplicationEventPublisher,
  private val transactionManager: PlatformTransactionManager,
  private val batchJobService: BatchJobService,
  private val atomicProgressState: AtomicProgressState
) : Logging {

  fun handleProgress(execution: BatchJobChunkExecution) {
    val job = batchJobService.getJobDto(execution.batchJob.id)
    val (progress, chunks) = atomicProgressState.withAtomicState(
      job.id,
      execution.id
    ) { progressAtomicLong, chunksAtomicLong ->
      val completedChunks: Long = if (!execution.retry)
        chunksAtomicLong.addAndGet(1)
      else chunksAtomicLong.get()

      val progress = progressAtomicLong.addAndGet(execution.successTargets.size.toLong())

      progress to completedChunks
    }

    if (execution.successTargets.isNotEmpty()) {
      eventPublisher.publishEvent(OnBatchOperationProgress(job, progress, job.totalItems.toLong()))
    }
    handleJobStatus(job, progress = progress, completedChunks = chunks)
  }

  fun handleChunkCompletedCommitted(execution: BatchJobChunkExecution) {
    if (execution.retry) {
      return
    }
    atomicProgressState.getCompletedChunksCommittedAtomicLong(execution.batchJob.id).addAndGet(1)
  }

  fun handleJobStatus(job: BatchJobDto, completedChunks: Long, progress: Long) {
    logger.debug("Job ${job.id} completed chunks: $completedChunks of ${job.totalChunks}")

    if (job.totalChunks.toLong() != completedChunks) {
      return
    }

    logger.debug("Job ${job.id} progress: $progress of ${job.totalItems}")

    val jobEntity = batchJobService.getJobEntity(job.id)
    if (job.totalItems.toLong() != progress) {
      jobEntity.status = BatchJobStatus.FAILED
      entityManager.persist(jobEntity)
      eventPublisher.publishEvent(OnBatchOperationFailed(job))
      return
    }

    jobEntity.status = BatchJobStatus.SUCCESS
    logger.debug("Publishing success event for job ${job.id}")
    eventPublisher.publishEvent(OnBatchOperationSucceeded(job))
    entityManager.persist(jobEntity)
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
        // let's not keep the locked when we handle the status
        val (progress, chunks) = atomicProgressState.getAtomicState(job.id)
        handleJobStatus(BatchJobDto.fromEntity(job), progress, chunks)
      }
    }
  }

  fun publishChunkProgress(jobId: Long, it: Int) {
    val job = batchJobService.getJobDto(jobId)
    eventPublisher.publishEvent(OnBatchOperationProgress(job, it.toLong(), job.totalItems.toLong()))
  }
}
