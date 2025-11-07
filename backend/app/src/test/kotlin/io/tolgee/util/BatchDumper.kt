package io.tolgee.util

import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.batch.BatchJob
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class BatchDumper(
  private val batchJobService: BatchJobService,
  private val batchJobStateProvider: BatchJobStateProvider,
  private val currentDateProvider: CurrentDateProvider,
  private val batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue,
  private val entityManager: EntityManager,
) : Logging {
  fun dump(jobId: Long) {
    val stringBuilder = StringBuilder()
    stringBuilder.append("Dumping job $jobId:")
    dumpQueuedItems(jobId, stringBuilder)
    dumpCachedState(stringBuilder, jobId)
    dumpDbExecutions(jobId, stringBuilder)
    logger.info(stringBuilder.toString())
  }

  private fun dumpDbExecutions(
    jobId: Long,
    stringBuilder: StringBuilder,
  ) {
    val dbExecutions = batchJobService.getExecutions(jobId)
    stringBuilder.append("\n\nDatabase state:")
    stringBuilder.append(" (${dbExecutions.size} executions)")
    stringBuilder.append("\n\n${listOf("Execution ID", "Status", "Completed", "ExecuteAfter offset").toTable()}")
    val dbExecutionsString =
      dbExecutions
        .joinToString(separator = "\n") {
          listOf(
            it.id,
            it.status.name,
            it.status.completed,
            it.executeAfter.offset,
          ).toTable()
        }
    stringBuilder.append("\n$dbExecutionsString")
  }

  private fun dumpCachedState(
    stringBuilder: StringBuilder,
    jobId: Long,
  ) {
    stringBuilder.append("\n\nCached state:")
    val cachedState = batchJobStateProvider.getCached(jobId)?.entries
    if (cachedState == null) {
      stringBuilder.append("\nNo cached state")
    } else {
      stringBuilder.append(" (${cachedState.size} executions)")
      val headers = listOf("Execution ID", "Status", "Completed", "Transaction committed").toTable()
      val cachedStateString =
        cachedState.joinToString(separator = "\n") {
          listOf(
            it.key,
            it.value.status.name,
            it.value.status.completed,
            it.value.transactionCommitted,
          ).toTable()
        }
      stringBuilder.append("\n\n$headers\n$cachedStateString")
    }
  }

  fun <T> finallyDump(fn: () -> T): T {
    return try {
      fn()
    } finally {
      this.dump(getSingleJob().id)
    }
  }

  fun <T> finallyDumpAll(fn: () -> T): T {
    return try {
      fn()
    } finally {
      getAllJobs().forEach {
        this.dump(it.id)
      }
    }
  }

  fun getSingleJob(): BatchJob = entityManager.createQuery("""from BatchJob""", BatchJob::class.java).singleResult

  fun getAllJobs(): List<BatchJob> = entityManager.createQuery("""from BatchJob""", BatchJob::class.java).resultList

  private fun dumpQueuedItems(
    jobId: Long,
    stringBuilder: StringBuilder,
  ) {
    val queuedItems = batchJobChunkExecutionQueue.getQueuedJobItems(jobId)
    val queuedItemsString = queuedItems.joinToString(", ")
    stringBuilder.append("\n\nQueued items: (${queuedItems.size} executions) $queuedItemsString")
  }

  private fun Collection<Any?>.toTable() = this.joinToString("\t\t|")

  private val java.util.Date?.offset: Long?
    get() {
      this ?: return null
      return this.time - currentDateProvider.date.time
    }
}
