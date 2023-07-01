package io.tolgee.batch

import io.tolgee.batch.events.OnBatchJobCancelled
import io.tolgee.batch.events.OnBatchJobFailed
import io.tolgee.batch.events.OnBatchJobProgress
import io.tolgee.batch.events.OnBatchJobSucceeded
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.batch.state.ExecutionState
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import javax.persistence.EntityManager

@Component
class ProgressManager(
  private val entityManager: EntityManager,
  private val eventPublisher: ApplicationEventPublisher,
  private val transactionManager: PlatformTransactionManager,
  private val batchJobService: BatchJobService,
  private val batchJobStateProvider: BatchJobStateProvider,
  private val cachingBatchJobService: CachingBatchJobService
) : Logging {

  fun handleProgress(execution: BatchJobChunkExecution) {
    val job = batchJobService.getJobDto(execution.batchJob.id)

    val info = batchJobStateProvider.updateState(job.id) {
      it[execution.id] =
        ExecutionState(
          successTargets = execution.successTargets,
          status = execution.status,
          chunkNumber = execution.chunkNumber,
          retry = execution.retry,
          transactionCommitted = false
        )

      it.getInfoForJobResult()
    }

    if (execution.successTargets.isNotEmpty()) {
      eventPublisher.publishEvent(OnBatchJobProgress(job, info.progress, job.totalItems.toLong()))
    }

    handleJobStatus(
      job,
      progress = info.progress,
      isAnyCancelled = info.isAnyCancelled,
      completedChunks = info.completedChunks
    )
  }

  fun handleChunkCompletedCommitted(execution: BatchJobChunkExecution) {
    batchJobStateProvider.updateState(execution.batchJob.id) {
      it.compute(execution.id) { _, v ->
        v?.copy(transactionCommitted = true)
      }
    }
  }

  fun handleJobStatus(
    job: BatchJobDto,
    progress: Long,
    completedChunks: Long,
    isAnyCancelled: Boolean
  ) {
    logger.debug("Job ${job.id} completed chunks: $completedChunks of ${job.totalChunks}")
    logger.debug("Job ${job.id} progress: $progress of ${job.totalItems}")

    if (job.totalChunks.toLong() != completedChunks) {
      return
    }

    val jobEntity = batchJobService.getJobEntity(job.id)
    if (isAnyCancelled) {
      jobEntity.status = BatchJobStatus.CANCELLED
      cachingBatchJobService.saveJob(jobEntity)
      eventPublisher.publishEvent(OnBatchJobCancelled(jobEntity.dto))
      return
    }

    if (job.totalItems.toLong() != progress) {
      jobEntity.status = BatchJobStatus.FAILED
      cachingBatchJobService.saveJob(jobEntity)
      eventPublisher.publishEvent(OnBatchJobFailed(jobEntity.dto))
      return
    }

    jobEntity.status = BatchJobStatus.SUCCESS
    logger.debug("Publishing success event for job ${job.id}")
    eventPublisher.publishEvent(OnBatchJobSucceeded(jobEntity.dto))
    cachingBatchJobService.saveJob(jobEntity)
  }

  fun Map<Long, ExecutionState>.getInfoForJobResult(): JobResultInfo {
    var completedChunks = 0L
    var progress = 0L
    this.values.forEach {
      if (it.status.completed && !it.retry) completedChunks++
      progress += it.successTargets.size
    }
    val isAnyCancelled = this.values.any { it.status == BatchJobChunkExecutionStatus.CANCELLED }
    return JobResultInfo(completedChunks, progress, isAnyCancelled)
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
        val state = batchJobStateProvider.get(job.id)
        val info = state.getInfoForJobResult()
        // let's not keep the locked when we handle the status
        handleJobStatus(
          BatchJobDto.fromEntity(job),
          progress = info.progress,
          completedChunks = info.completedChunks,
          info.isAnyCancelled
        )
      }
    }
  }

  fun getJobCachedProgress(jobId: Long): Long? {
    return batchJobStateProvider.getCached(jobId)?.getInfoForJobResult()?.progress
  }

  fun publishSingleChunkProgress(jobId: Long, progress: Int) {
    val job = batchJobService.getJobDto(jobId)
    eventPublisher.publishEvent(OnBatchJobProgress(job, progress.toLong(), job.totalItems.toLong()))
  }

  fun handleJobRunning(id: Long) {
    executeInNewTransaction(transactionManager, isolationLevel = TransactionDefinition.ISOLATION_DEFAULT) {
      logger.trace("""Fetching job $id""")
      val job = batchJobService.getJobDto(id)
      if (job.status == BatchJobStatus.PENDING) {
        logger.debug("""Updating job state to running ${job.id}""")
        cachingBatchJobService.setRunningState(job.id)
      }
    }
  }

  data class JobResultInfo(val completedChunks: Long, val progress: Long, val isAnyCancelled: Boolean)
}
