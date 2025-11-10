package io.tolgee.util

import io.tolgee.model.Project
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager

@Component
class StuckBatchJobTestUtil(
  private val transactionManager: PlatformTransactionManager,
  private val entityManager: EntityManager,
) {
  fun createBatchJobWithExecutionStatuses(
    project: Project,
    batchJobStatus: BatchJobStatus,
    executionStatuses: Set<BatchJobChunkExecutionStatus>,
  ): BatchJob {
    return createBatchJobWithExecutionStatuses(
      project = project,
      batchJobStatus = batchJobStatus,
      executionStatuses =
        executionStatuses
          .mapIndexed { index, status ->
            index to listOf(status)
          }.toMap(),
    )
  }

  fun createBatchJobWithExecutionStatuses(
    project: Project,
    batchJobStatus: BatchJobStatus,
    executionStatuses: Map<Int, List<BatchJobChunkExecutionStatus>>,
  ): BatchJob {
    return executeInNewTransaction(transactionManager) {
      val job =
        BatchJob().apply {
          status = batchJobStatus
          this.project = project
          totalChunks = executionStatuses.size
        }

      entityManager.persist(job)

      executionStatuses.map { (chunkNumber, statuses) ->
        statuses.map { status ->
          BatchJobChunkExecution().apply {
            batchJob = job
            this.status = status
            this.chunkNumber = chunkNumber
            entityManager.persist(this)
          }
        }
      }
      job
    }
  }
}
