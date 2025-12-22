package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.events.OnBatchJobCancelled
import io.tolgee.batch.events.OnBatchJobFailed
import io.tolgee.batch.events.OnBatchJobProgress
import io.tolgee.batch.events.OnBatchJobStatusUpdated
import io.tolgee.batch.events.OnBatchJobSucceeded
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.batch.state.ExecutionState
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.util.Logging
import io.tolgee.util.debug
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.util.trace
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
  private val queue: BatchJobChunkExecutionQueue,
) : Logging {
  /**
   * This method tries to set execution running in the state
   * @param canRunFn function that returns true if execution can be run
   */
  fun trySetExecutionRunning(
    executionId: Long,
    batchJobId: Long,
    canRunFn: (Map<Long, ExecutionState>) -> Boolean,
  ): Boolean {
    return batchJobStateProvider.updateState(batchJobId) {
      if (canRunFn(it)) {
        if (it[executionId] != null) {
          val currentState = it[executionId]
          // Don't overwrite terminal states
          if (currentState?.status?.completed == true) {
            return@updateState false
          }
          currentState?.status = BatchJobChunkExecutionStatus.RUNNING
          return@updateState true
        }
        it[executionId] =
          ExecutionState(
            successTargets = listOf(),
            status = BatchJobChunkExecutionStatus.RUNNING,
            chunkNumber = null,
            retry = null,
            transactionCommitted = false,
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

  fun handleProgress(
    execution: BatchJobChunkExecution,
    failOnly: Boolean = false,
  ) {
    val job = batchJobService.getJobDto(execution.batchJob.id)

    // the update state function locks it, so we can sure nothing run concurrently there,
    // this might be a source of bugs
    batchJobStateProvider.updateState(job.id) {
      it[execution.id] = batchJobStateProvider.getStateForExecution(execution)

      val info = it.getInfoForJobResult()

      if (execution.successTargets.isNotEmpty()) {
        eventPublisher.publishEvent(OnBatchJobProgress(job, info.progress, job.totalItems.toLong()))
      }
      handleJobStatus(job, info)
    }
  }

  fun handleChunkCompletedCommitted(
    execution: BatchJobChunkExecution,
    triggerJobCompleted: Boolean = true,
  ) {
    val state =
      batchJobStateProvider.updateState(execution.batchJob.id) {
        logger.debug("Setting transaction committed for chunk execution ${execution.id} to true")
        it.compute(execution.id) { _, v ->
          val state = batchJobStateProvider.getStateForExecution(execution)
          state.transactionCommitted = true
          state
        }
        it
      }

    val isJobCompleted = state.all { it.value.transactionCommitted && it.value.status.completed }
    logger.debug {
      val incompleteExecutions =
        state
          .filter { !(it.value.transactionCommitted && it.value.status.completed) }
          .map { it.key }
          .joinToString(", ")
      "Is job ${execution.batchJob.id} " +
        "completed: $isJobCompleted " +
        "(current execution: ${execution.id}), " +
        "incomplete executions: $incompleteExecutions"
    }
    if (isJobCompleted && triggerJobCompleted) {
      onJobCompletedCommitted(execution.batchJob.id)
    }
  }

  fun onJobCompletedCommitted(jobId: Long) {
    val jobDto = batchJobService.getJobDto(jobId)
    onJobCompletedCommitted(jobDto)
  }

  fun onJobCompletedCommitted(batchJob: BatchJobDto) {
    eventPublisher.publishEvent(OnBatchJobStatusUpdated(batchJob.id, batchJob.projectId, batchJob.status))
    cachingBatchJobService.evictJobCache(batchJob.id)
    batchJobProjectLockingManager.unlockJobForProject(batchJob.projectId, batchJob.id)
    batchJobStateProvider.removeJobState(batchJob.id)
  }

  fun handleJobStatus(
    job: BatchJobDto,
    info: JobResultInfo,
  ) {
    if (job.totalChunks != info.completedChunks) {
      return
    }

    val jobEntity = batchJobService.getJobEntity(job.id)

    if (info.isAnyFailed) {
      jobEntity.status = BatchJobStatus.FAILED
      logger.debug("""Saving job with Failed status""")
      cachingBatchJobService.saveJob(jobEntity)
      val safeErrorMessage = batchJobService.getErrorMessages(listOf(jobEntity))[job.id]
      eventPublisher.publishEvent(OnBatchJobFailed(jobEntity.dto, safeErrorMessage))
      return
    }

    if (info.isAnyCancelled) {
      jobEntity.status = BatchJobStatus.CANCELLED
      logger.debug("""Saving job with Cancelled status""")
      cachingBatchJobService.saveJob(jobEntity)
      eventPublisher.publishEvent(OnBatchJobCancelled(jobEntity.dto))
      return
    }

    jobEntity.status = BatchJobStatus.SUCCESS
    logger.debug { "Publishing success event for job ${job.id}" }
    eventPublisher.publishEvent(OnBatchJobSucceeded(jobEntity.dto))
    cachingBatchJobService.saveJob(jobEntity)
  }

  fun Map<Long, ExecutionState>.getInfoForJobResult(): JobResultInfo {
    var completedChunks = 0
    var progress = 0L
    this.values.forEach {
      if (it.status.completed && it.retry == false) completedChunks++
      progress += it.successTargets.size
    }
    val isAnyCancelled = this.values.any { it.status == BatchJobChunkExecutionStatus.CANCELLED }
    val isAnyFailed = this.values.any { it.status == BatchJobChunkExecutionStatus.FAILED && it.retry == false }
    return JobResultInfo(completedChunks, progress, isAnyCancelled, isAnyFailed)
  }

  fun getJobCachedProgress(jobId: Long): Long? {
    return batchJobStateProvider.getCached(jobId)?.getInfoForJobResult()?.progress
  }

  fun publishSingleChunkProgress(
    jobId: Long,
    progress: Int,
  ) {
    val job = batchJobService.getJobDto(jobId)
    eventPublisher.publishEvent(OnBatchJobProgress(job, progress.toLong(), job.totalItems.toLong()))
  }

  fun handleJobRunning(id: Long) {
    executeInNewTransaction(transactionManager) {
      logger.trace { """Fetching job $id""" }
      val job = batchJobService.getJobDto(id)
      if (job.status == BatchJobStatus.PENDING) {
        logger.debug { """Updating job state to running ${job.id}""" }
        cachingBatchJobService.setRunningState(job.id)
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
    val jobDto = batchJobService.getJobDto(jobId)

    val isCompleted = jobDto.status.completed

    if (isCompleted) {
      logger.debug("Job $jobId is completed, unlocking project, removing job state")
      queue.removeJobExecutions(jobId)
      batchJobProjectLockingManager.unlockJobForProject(jobDto.projectId, jobId)
      batchJobStateProvider.removeJobState(jobId)
    }
  }

  data class JobResultInfo(
    val completedChunks: Int,
    val progress: Long,
    val isAnyCancelled: Boolean,
    val isAnyFailed: Boolean,
  )
}
