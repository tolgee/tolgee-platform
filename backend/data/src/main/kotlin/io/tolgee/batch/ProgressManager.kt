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
   */
  fun trySetExecutionRunning(
    executionId: Long,
    batchJobId: Long,
  ): Boolean {
    // Ensure state is initialized (O(1) check after first call)
    batchJobStateProvider.ensureInitialized(batchJobId)

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
   * it doesn't set it when the status is different from RUNNING.
   * Always decrements the running count since it was already incremented in trySetExecutionRunning.
   */
  fun rollbackSetToRunning(
    executionId: Long,
    batchJobId: Long,
  ) {
    // Lock-free: check and remove single execution only if still RUNNING
    val currentState = batchJobStateProvider.getSingleExecution(batchJobId, executionId)
    if (currentState?.status == BatchJobChunkExecutionStatus.RUNNING) {
      batchJobStateProvider.removeSingleExecution(batchJobId, executionId)
    }
    // Always decrement running count - it was incremented in trySetExecutionRunning
    // and needs to be rolled back regardless of whether another worker changed the state
    batchJobStateProvider.decrementRunningCount(batchJobId)
  }

  /**
   * Called when the coroutine executing a chunk finishes (via invokeOnCompletion).
   * This decrements the running count to align with actual coroutine lifecycle.
   */
  fun onExecutionCoroutineComplete(batchJobId: Long) {
    batchJobStateProvider.decrementRunningCount(batchJobId)
  }

  fun handleProgress(
    execution: BatchJobChunkExecution,
    batchJobDto: BatchJobDto? = null,
  ) {
    val job = batchJobDto ?: batchJobService.getJobDto(execution.batchJob.id)

    // Get existing state to preserve transactionCommitted and detect status changes
    val existingState = batchJobStateProvider.getSingleExecution(job.id, execution.id)

    // Track if this execution just became "countable as completed"
    // An execution is countable when it's completed AND retry=false
    val wasCountableAsCompleted = existingState?.status?.completed == true && existingState.retry != true
    val isNowCountableAsCompleted = execution.status.completed && execution.retry != true

    // Create new state preserving transactionCommitted
    val newState = batchJobStateProvider.getStateForExecution(execution)
    newState.transactionCommitted = existingState?.transactionCommitted ?: false

    // Update single execution state (lock-free)
    batchJobStateProvider.updateSingleExecution(job.id, execution.id, newState)

    // Update progress counter
    if (execution.successTargets.isNotEmpty()) {
      batchJobStateProvider.addProgressCount(job.id, execution.successTargets.size.toLong())
    }

    // Only update completion-related counters if status changed to countable
    if (isNowCountableAsCompleted && !wasCountableAsCompleted) {
      batchJobStateProvider.incrementCompletedChunksCount(job.id)

      if (execution.status == BatchJobChunkExecutionStatus.FAILED) {
        batchJobStateProvider.incrementFailedCount(job.id)
      }
      if (execution.status == BatchJobChunkExecutionStatus.CANCELLED) {
        batchJobStateProvider.incrementCancelledCount(job.id)
      }
    }

    // Use counters for job result info (O(1) operations)
    val completedChunks = batchJobStateProvider.getCompletedChunksCount(job.id)
    val info = JobResultInfo(
      completedChunks = completedChunks,
      progress = batchJobStateProvider.getProgressCount(job.id),
      isAnyCancelled = batchJobStateProvider.getCancelledCount(job.id) > 0,
      isAnyFailed = batchJobStateProvider.getFailedCount(job.id) > 0,
    )

    if (execution.successTargets.isNotEmpty()) {
      eventPublisher.publishEvent(OnBatchJobProgress(job, info.progress, job.totalItems.toLong()))
    }
    handleJobStatus(job, info)
  }

  fun handleChunkCompletedCommitted(
    execution: BatchJobChunkExecution,
    triggerJobCompleted: Boolean = true,
    batchJobDto: BatchJobDto? = null,
  ) {
    logger.debug("Setting transaction committed for chunk execution ${execution.id} to true")

    // Idempotency guard: check if already marked as committed to prevent double-counting
    val existingState = batchJobStateProvider.getSingleExecution(execution.batchJob.id, execution.id)
    if (existingState?.transactionCommitted == true) {
      logger.debug("Execution ${execution.id} already marked as transaction committed, skipping")
      return
    }

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
    val job = batchJobDto ?: batchJobService.getJobDto(execution.batchJob.id)
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

  fun handleJobRunning(batchJobDto: BatchJobDto) {
    if (batchJobDto.status == BatchJobStatus.PENDING) {
      executeInNewTransaction(transactionManager) {
        logger.debug { """Updating job state to running ${batchJobDto.id}""" }
        cachingBatchJobService.setRunningState(batchJobDto.id)
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
