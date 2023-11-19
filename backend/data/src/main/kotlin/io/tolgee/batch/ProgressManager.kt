package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.events.*
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.batch.state.ExecutionState
import io.tolgee.constants.Message
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager

@Component
class ProgressManager(
  private val eventPublisher: ApplicationEventPublisher,
  private val transactionManager: PlatformTransactionManager,
  private val batchJobService: BatchJobService,
  private val batchJobStateProvider: BatchJobStateProvider,
  private val cachingBatchJobService: CachingBatchJobService,
  private val batchJobProjectLockingManager: BatchJobProjectLockingManager,
  private val queue: BatchJobChunkExecutionQueue
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
      if (canRunFn(it)) {
        if (it[executionId] != null) {
          return@updateState true
        }
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

  fun handleProgress(execution: BatchJobChunkExecution, failOnly: Boolean = false) {
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
      errorMessage = execution.errorMessage,
      failOnly = failOnly
    )
  }

  fun handleChunkCompletedCommitted(execution: BatchJobChunkExecution) {
    val state = batchJobStateProvider.updateState(execution.batchJob.id) {
      logger.debug("Setting transaction committed for chunk execution ${execution.id} to true")
      it.compute(execution.id) { _, v ->
        val state = batchJobStateProvider.getStateForExecution(execution)
        state.transactionCommitted = true
        state
      }
      it
    }
    val isJobCompleted = state.all { it.value.transactionCommitted && it.value.status.completed }
    logger.debug("Is job ${execution.batchJob.id} completed: $isJobCompleted (execution: ${execution.id})")
    if (isJobCompleted) {
      onJobCompletedCommitted(execution)
    }
  }

  private fun onJobCompletedCommitted(execution: BatchJobChunkExecution) {
    batchJobStateProvider.removeJobState(execution.batchJob.id)
    val jobDto = batchJobService.getJobDto(execution.batchJob.id)
    cachingBatchJobService.evictJobCache(execution.batchJob.id)
    batchJobProjectLockingManager.unlockJobForProject(jobDto.projectId, jobDto.id)
    batchJobStateProvider.removeJobState(execution.batchJob.id)
  }

  fun handleJobStatus(
    job: BatchJobDto,
    progress: Long,
    completedChunks: Long,
    isAnyCancelled: Boolean,
    errorMessage: Message? = null,
    failOnly: Boolean = false
  ) {
    logger.debug("Job ${job.id} completed chunks: $completedChunks of ${job.totalChunks}")
    logger.debug("Job ${job.id} progress: $progress of ${job.totalItems}")

    if (job.totalChunks.toLong() != completedChunks) {
      return
    }

    val jobEntity = batchJobService.getJobEntity(job.id)

    if (isAnyCancelled && !failOnly) {
      jobEntity.status = BatchJobStatus.CANCELLED
      logger.debug("""Saving job with Cancelled status""")
      cachingBatchJobService.saveJob(jobEntity)
      eventPublisher.publishEvent(OnBatchJobCancelled(jobEntity.dto))
      return
    }

    if (job.totalItems.toLong() != progress || failOnly) {
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

  fun getJobCachedProgress(jobId: Long): Long? {
    return batchJobStateProvider.getCached(jobId)?.getInfoForJobResult()?.progress
  }

  fun publishSingleChunkProgress(jobId: Long, progress: Int) {
    val job = batchJobService.getJobDto(jobId)
    eventPublisher.publishEvent(OnBatchJobProgress(job, progress.toLong(), job.totalItems.toLong()))
  }

  fun handleJobRunning(id: Long) {
    executeInNewTransaction(transactionManager) {
      logger.trace("""Fetching job $id""")
      val job = batchJobService.getJobDto(id)
      if (job.status == BatchJobStatus.PENDING) {
        logger.debug("""Updating job state to running ${job.id}""")
        cachingBatchJobService.setRunningState(job.id)
        eventPublisher.publishEvent(OnBatchJobStarted(job))
      }
    }
  }

  /**
   * It can happen that some other thread or instance will try to
   * execute execution of already completed job
   *
   * The execution is skipped, since it's not pending, but
   * we have to unlock the project, otherwise it will be locked forever
   */
  fun finalizeIfCompleted(jobId: Long) {
    val cached = batchJobStateProvider.getCached(jobId)
    logger.debug("Checking if job $jobId is completed, has cached value: ${cached != null}")
    val isCompleted = cached?.all { it.value.status.completed } ?: batchJobService.getJobDto(jobId).status.completed
    if (isCompleted) {
      val jobDto = batchJobService.getJobDto(jobId)
      logger.debug("Job $jobId is completed, unlocking project, removing job state")
      queue.removeJobExecutions(jobId)
      batchJobProjectLockingManager.unlockJobForProject(jobDto.projectId, jobId)
      batchJobStateProvider.removeJobState(jobId)
    }
  }

  data class JobResultInfo(val completedChunks: Long, val progress: Long, val isAnyCancelled: Boolean)
}
