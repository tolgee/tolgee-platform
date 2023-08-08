package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.events.OnBatchJobCancelled
import io.tolgee.batch.events.OnBatchJobFailed
import io.tolgee.batch.events.OnBatchJobProgress
import io.tolgee.batch.events.OnBatchJobStatusUpdated
import io.tolgee.batch.events.OnBatchJobSucceeded
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.batch.state.ExecutionState
import io.tolgee.constants.Message
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
  private val cachingBatchJobService: CachingBatchJobService,
  private val batchJobProjectLockingManager: BatchJobProjectLockingManager
) : Logging {

  /**
   * This method tries to set execution running in the state
   * @param canRunFn function that returns true if execution can be run
   */
  fun trySetExecutionRunning(
    executionId: Long,
    batchJobId: Long,
    canRunFn: (Map<Long, ExecutionState>) -> Boolean
  ): Boolean {
    return batchJobStateProvider.updateState(batchJobId) {
      if (it[executionId] != null) {
        // we expect the item wasn't touched by others
        // if it was, there are other mechanisms to handle it,
        // so we just ignore it
        return@updateState true
      }
      if (canRunFn(it)) {
        it[executionId] =
          ExecutionState(
            successTargets = listOf(),
            status = BatchJobChunkExecutionStatus.RUNNING,
            chunkNumber = null,
            retry = null,
            transactionCommitted = false
          )
        return@updateState true
      }
      return@updateState false
    }
  }

  /**
   * This method is called when the execution is not even started, because it was locked or something,
   * it doesn't set it when the status is different from RUNNING
   */
  fun rollbackSetToRunning(
    executionId: Long,
    batchJobId: Long,
  ) {
    return batchJobStateProvider.updateState(batchJobId) {
      if (it[executionId]?.status == BatchJobChunkExecutionStatus.RUNNING) {
        it.remove(executionId)
      }
    }
  }

  fun handleProgress(execution: BatchJobChunkExecution) {
    val job = batchJobService.getJobDto(execution.batchJob.id)

    val info = batchJobStateProvider.updateState(job.id) {
      it[execution.id] = batchJobStateProvider.getStateForExecution(execution)
      it.getInfoForJobResult()
    }

    if (execution.successTargets.isNotEmpty()) {
      eventPublisher.publishEvent(OnBatchJobProgress(job, info.progress, job.totalItems.toLong()))
    }

    handleJobStatus(
      job,
      progress = info.progress,
      isAnyCancelled = info.isAnyCancelled,
      completedChunks = info.completedChunks,
      errorMessage = execution.errorMessage
    )
  }

  fun handleChunkCompletedCommitted(execution: BatchJobChunkExecution) {
    val state = batchJobStateProvider.updateState(execution.batchJob.id) {
      logger.debug("Setting transaction committed for chunk execution ${execution.id} to true")
      it.compute(execution.id) { _, v ->
        v?.copy(transactionCommitted = true)
      }
      it
    }
    val isJobCompleted = state.all { it.value.transactionCommitted && it.value.status.completed }
    if (isJobCompleted) {
      onJobCompletedCommitted(execution)
    }
  }

  private fun onJobCompletedCommitted(execution: BatchJobChunkExecution) {
    batchJobStateProvider.removeJobState(execution.batchJob.id)
    val jobDto = batchJobService.getJobDto(execution.batchJob.id)
    batchJobProjectLockingManager.unlockJobForProject(jobDto.projectId)
    eventPublisher.publishEvent(OnBatchJobStatusUpdated(jobDto.id, jobDto.projectId, jobDto.status))
    cachingBatchJobService.evictJobCache(execution.batchJob.id)
  }

  fun handleJobStatus(
    job: BatchJobDto,
    progress: Long,
    completedChunks: Long,
    isAnyCancelled: Boolean,
    errorMessage: Message? = null
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
      val safeErrorMessage = errorMessage ?: batchJobService.getErrorMessages(listOf(jobEntity))[job.id]
      eventPublisher.publishEvent(OnBatchJobFailed(jobEntity.dto, safeErrorMessage))
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
      if (it.status.completed && it.retry == false) completedChunks++
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
