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
   * This method tries to set execution running in the state.
   * Lock-free O(1) implementation using atomic counter and single-execution operations.
   * @param canRunFn function that returns true if execution can be run (called with current state)
   */
  fun trySetExecutionRunning(
    executionId: Long,
    batchJobId: Long,
    canRunFn: (Map<Long, ExecutionState>) -> Boolean,
  ): Boolean {
    // Ensure state is initialized (O(1) check after first call)
    batchJobStateProvider.ensureInitialized(batchJobId)

    // Check if caller allows running (provides state for backward compatibility)
    val state = batchJobStateProvider.get(batchJobId)
    if (!canRunFn(state)) {
      return false
    }

    // Increment running count (will be decremented in onExecutionCoroutineComplete)
    batchJobStateProvider.incrementRunningCount(batchJobId)

    // O(1) check if execution already exists with terminal state
    val currentState = batchJobStateProvider.getSingleExecution(batchJobId, executionId)
    if (currentState?.status?.completed == true) {
      // Already completed - decrement running count and reject
      batchJobStateProvider.decrementRunningCount(batchJobId)
      return false
    }

    // O(1) set to RUNNING using atomic single-execution update
    batchJobStateProvider.updateSingleExecution(
      batchJobId,
      executionId,
      ExecutionState(
        successTargets = currentState?.successTargets ?: listOf(),
        status = BatchJobChunkExecutionStatus.RUNNING,
        chunkNumber = currentState?.chunkNumber,
        retry = currentState?.retry,
        transactionCommitted = currentState?.transactionCommitted ?: false,
      ),
    )
    return true
  }

  /**
   * This method is called when the execution is not even started, because it was locked or something,
   * it doesn't set it when the status is different from RUNNING
   */
  fun rollbackSetToRunning(
    executionId: Long,
    batchJobId: Long,
  ) {
    // Lock-free: check and remove single execution
    val currentState = batchJobStateProvider.getSingleExecution(batchJobId, executionId)
    if (currentState?.status == BatchJobChunkExecutionStatus.RUNNING) {
      batchJobStateProvider.removeSingleExecution(batchJobId, executionId)
      batchJobStateProvider.decrementRunningCount(batchJobId)
    }
  }

  /**
   * Called when the coroutine executing a chunk finishes (via invokeOnCompletion).
   * This decrements the running count to align with actual coroutine lifecycle.
   */
  fun onExecutionCoroutineComplete(batchJobId: Long) {
    batchJobStateProvider.decrementRunningCount(batchJobId)
  }

  fun handleProgress(execution: BatchJobChunkExecution) {
    val job = batchJobService.getJobDto(execution.batchJob.id)

    // Lock-free: update single execution using atomic Redis HSET
    batchJobStateProvider.updateSingleExecution(
      job.id,
      execution.id,
      batchJobStateProvider.getStateForExecution(execution),
    )

    // Update counters atomically
    if (execution.successTargets.isNotEmpty()) {
      batchJobStateProvider.addProgressCount(job.id, execution.successTargets.size.toLong())
    }

    if (execution.status.completed) {
      // NOTE: Running count is NOT decremented here. It's decremented in onExecutionCoroutineComplete
      // which is called when the coroutine actually finishes (via invokeOnCompletion).
      // This ensures the running count aligns with actual coroutine lifecycle, not just execution state.
      if (execution.retry != true) {
        val beforeCount = batchJobStateProvider.getCompletedChunksCount(job.id)
        batchJobStateProvider.incrementCompletedChunksCount(job.id)
        val afterCount = batchJobStateProvider.getCompletedChunksCount(job.id)
        logger.info("incrementCompletedChunksCount: job=${job.id}, execution=${execution.id}, before=$beforeCount, after=$afterCount, retry=${execution.retry}")
      } else {
        logger.info("Skipping increment for job=${job.id}, execution=${execution.id}, retry=${execution.retry}")
      }
    }
    if (execution.status == BatchJobChunkExecutionStatus.FAILED && execution.retry != true) {
      batchJobStateProvider.incrementFailedCount(job.id)
    }
    if (execution.status == BatchJobChunkExecutionStatus.CANCELLED) {
      batchJobStateProvider.incrementCancelledCount(job.id)
    }

    // Read counters for job result info (O(1) operations)
    val completedChunks = batchJobStateProvider.getCompletedChunksCount(job.id)
    val info = JobResultInfo(
      completedChunks = completedChunks,
      progress = batchJobStateProvider.getProgressCount(job.id),
      isAnyCancelled = batchJobStateProvider.getCancelledCount(job.id) > 0,
      isAnyFailed = batchJobStateProvider.getFailedCount(job.id) > 0,
    )

    if (completedChunks >= job.totalChunks - 5) {
      logger.info("handleProgress: job=${job.id}, completedChunks=$completedChunks, totalChunks=${job.totalChunks}")
    }

    if (execution.successTargets.isNotEmpty()) {
      eventPublisher.publishEvent(OnBatchJobProgress(job, info.progress, job.totalItems.toLong()))
    }
    handleJobStatus(job, info)
  }

  fun handleChunkCompletedCommitted(
    execution: BatchJobChunkExecution,
    triggerJobCompleted: Boolean = true,
  ) {
    logger.debug("Setting transaction committed for chunk execution ${execution.id} to true")

    // Lock-free: update single execution using atomic Redis HSET
    val executionState = batchJobStateProvider.getStateForExecution(execution)
    executionState.transactionCommitted = true
    batchJobStateProvider.updateSingleExecution(
      execution.batchJob.id,
      execution.id,
      executionState,
    )

    // Only count executions that are NOT going to be retried.
    // Executions with retry=true are intermediate failures, not the final state of that chunk.
    if (execution.retry) {
      logger.debug {
        "Skipping committed count increment for execution ${execution.id} (retry=true)"
      }
      return
    }

    // Atomically increment committed counter and get the new value
    // This ensures only the thread that reaches totalChunks will trigger job completion
    val committedCount = batchJobStateProvider.incrementCommittedCountAndGet(execution.batchJob.id)
    val job = batchJobService.getJobDto(execution.batchJob.id)
    val isJobCompleted = committedCount == job.totalChunks

    logger.debug {
      "Is job ${execution.batchJob.id} completed: $isJobCompleted " +
        "(committed: $committedCount/${job.totalChunks}, current execution: ${execution.id})"
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

  fun getJobCachedProgress(jobId: Long): Long? {
    if (!batchJobStateProvider.hasCachedJobState(jobId)) {
      return null
    }
    return batchJobStateProvider.getProgressCount(jobId)
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
