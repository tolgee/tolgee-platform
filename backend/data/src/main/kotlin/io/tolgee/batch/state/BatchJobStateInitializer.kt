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
    val rows =
      executeInNewTransaction(
        platformTransactionManager,
        isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED,
        readOnly = true,
      ) {
        @Suppress("UNCHECKED_CAST")
        entityManager
          .createNativeQuery(
            """
            select id, status, chunk_number, retry, coalesce(jsonb_array_length(success_targets), 0)
            from tolgee_batch_job_chunk_execution
            where batch_job_id = :jobId
            """,
          ).setParameter("jobId", jobId)
          .resultList as List<Array<Any?>>
      }

    return rows
      .associate { row ->
        val status = BatchJobChunkExecutionStatus.valueOf(row[1] as String)
        (row[0] as Number).toLong() to
          ExecutionState(
            successTargetsCount = (row[4] as Number).toInt(),
            status = status,
            chunkNumber = (row[2] as Number?)?.toInt(),
            retry = row[3] as Boolean?,
            // Only mark as committed if the execution has already completed.
            // PENDING and RUNNING executions haven't committed their changes yet.
            transactionCommitted = status.completed,
          )
      }.toMutableMap()
  }

  fun getStateForExecution(execution: BatchJobChunkExecution): ExecutionState {
    return ExecutionState(
      successTargetsCount = execution.successTargets.size,
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
      progressCount += executionState.successTargetsCount
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
