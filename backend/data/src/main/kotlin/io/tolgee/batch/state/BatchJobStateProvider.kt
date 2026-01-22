package io.tolgee.batch.state

import io.tolgee.model.batch.BatchJobChunkExecution

/**
 * Interface for managing batch job execution state.
 * Implementations handle either local (in-memory) or Redis-based storage.
 */
interface BatchJobStateProvider {
  // Single execution operations
  fun updateSingleExecution(
    jobId: Long,
    executionId: Long,
    state: ExecutionState,
  )

  fun removeSingleExecution(
    jobId: Long,
    executionId: Long,
  )

  fun getSingleExecution(
    jobId: Long,
    executionId: Long,
  ): ExecutionState?

  fun ensureInitialized(jobId: Long)

  // Counter operations
  fun getRunningCount(jobId: Long): Int

  fun incrementRunningCount(jobId: Long)

  fun incrementRunningCountAndGet(jobId: Long): Int

  fun tryIncrementRunningCount(
    jobId: Long,
    maxConcurrency: Int,
  ): Boolean

  fun decrementRunningCount(jobId: Long)

  fun getCompletedChunksCount(jobId: Long): Int

  fun incrementCompletedChunksCount(jobId: Long)

  fun incrementCompletedChunksCountAndGet(jobId: Long): Int

  fun getProgressCount(jobId: Long): Long

  fun addProgressCount(
    jobId: Long,
    delta: Long,
  )

  fun getFailedCount(jobId: Long): Int

  fun incrementFailedCount(jobId: Long)

  fun getCancelledCount(jobId: Long): Int

  fun incrementCancelledCount(jobId: Long)

  fun getCommittedCount(jobId: Long): Int

  fun incrementCommittedCountAndGet(jobId: Long): Int

  // Job-level operations
  fun get(jobId: Long): MutableMap<Long, ExecutionState>

  fun getCached(jobId: Long): MutableMap<Long, ExecutionState>?

  fun removeJobState(jobId: Long): MutableMap<Long, ExecutionState>?

  fun hasCachedJobState(jobId: Long): Boolean

  fun getCachedJobIds(): MutableSet<Long>

  // Job lifecycle
  /**
   * Atomically marks a job as started. Returns true only the first time
   * this method is called for a given jobId, false for subsequent calls.
   * Used to ensure OnBatchJobStarted event fires exactly once per job.
   */
  fun tryMarkJobStarted(jobId: Long): Boolean

  // Cleanup & utility
  fun clearUnusedStates()

  fun clearAllState()

  fun getStateForExecution(execution: BatchJobChunkExecution): ExecutionState

  fun getInitialState(jobId: Long): MutableMap<Long, ExecutionState>
}
