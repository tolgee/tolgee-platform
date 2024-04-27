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
    return executeInNewTransaction(transactionManager) {
      val job =
        BatchJob().apply {
          status = batchJobStatus
          this.project = project
        }

      entityManager.persist(job)

      executionStatuses.map { status ->
        BatchJobChunkExecution().apply {
          batchJob = job
          this.status = status
          entityManager.persist(this)
        }
      }
      job
    }
  }
}
