package io.tolgee.batch.state

import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition

/**
 * Shared helper for initializing batch job state from the database.
 * Used by both local and Redis implementations.
 */
@Component
class BatchJobStateInitializer(
  private val entityManager: EntityManager,
  private val platformTransactionManager: PlatformTransactionManager,
) : Logging {
  fun getInitialState(jobId: Long): MutableMap<Long, ExecutionState> {
    logger.debug("Initializing batch job state for job $jobId")
    // we want to get state which is not affected by current transaction
    val executions =
      executeInNewTransaction(
        platformTransactionManager,
        isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED,
        readOnly = true,
      ) {
        entityManager
          .createQuery(
            """
      from BatchJobChunkExecution bjce
      where bjce.batchJob.id = :jobId
      """,
            BatchJobChunkExecution::class.java,
          ).setParameter("jobId", jobId)
          .resultList
      }

    return executions
      .associate {
        it.id to
          ExecutionState(
            it.successTargets,
            it.status,
            it.chunkNumber,
            it.retry,
            // Only mark as committed if the execution has already completed.
            // PENDING and RUNNING executions haven't committed their changes yet.
            it.status.completed,
          )
      }.toMutableMap()
  }

  fun getStateForExecution(execution: BatchJobChunkExecution): ExecutionState {
    return ExecutionState(
      successTargets = execution.successTargets,
      status = execution.status,
      chunkNumber = execution.chunkNumber,
      retry = execution.retry,
      transactionCommitted = false,
    )
  }

  /**
   * Calculates counter values from the given state map.
   * Used during initialization to set counters to match DB state.
   */
  fun calculateCountersFromState(state: Map<Long, ExecutionState>): CounterValues {
    var runningCount = 0
    var completedChunksCount = 0
    var progressCount = 0L
    var failedCount = 0
    var cancelledCount = 0
    var committedCount = 0

    state.values.forEach { executionState ->
      if (executionState.status == BatchJobChunkExecutionStatus.RUNNING) {
        runningCount++
      }
      if (executionState.status.completed && executionState.retry != true) {
        completedChunksCount++
      }
      progressCount += executionState.successTargets.size
      if (executionState.status == BatchJobChunkExecutionStatus.FAILED &&
        executionState.retry != true
      ) {
        failedCount++
      }
      if (executionState.status == BatchJobChunkExecutionStatus.CANCELLED) {
        cancelledCount++
      }
      if (executionState.transactionCommitted) {
        committedCount++
      }
    }

    return CounterValues(
      runningCount = runningCount,
      completedChunksCount = completedChunksCount,
      progressCount = progressCount,
      failedCount = failedCount,
      cancelledCount = cancelledCount,
      committedCount = committedCount,
    )
  }
}

/**
 * Data class holding counter values calculated from state.
 */
data class CounterValues(
  val runningCount: Int,
  val completedChunksCount: Int,
  val progressCount: Long,
  val failedCount: Int,
  val cancelledCount: Int,
  val committedCount: Int,
)
