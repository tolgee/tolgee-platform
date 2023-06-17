package io.tolgee.batch

import io.sentry.Sentry
import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hibernate.LockOptions
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import java.util.concurrent.ConcurrentLinkedQueue
import javax.annotation.PreDestroy
import javax.persistence.EntityManager
import javax.persistence.LockModeType

@Service
class BatchJobActionService(
  private val entityManager: EntityManager,
  private val currentDateProvider: CurrentDateProvider,
  private val transactionManager: PlatformTransactionManager,
  private val applicationContext: ApplicationContext,
  private val redisTemplate: StringRedisTemplate
) : Logging {
  companion object {
    const val CONCURRENCY = 10
  }

  var runJob: Job? = null
  var run = true

  var queue = ConcurrentLinkedQueue<ExecutionQueueItem>()
  var runningJobs = 0

  @EventListener(ApplicationReadyEvent::class)
  fun run() {
    executeInNewTransaction(transactionManager) {
      populateQueue()
    }

    @Suppress("OPT_IN_USAGE")
    runJob = GlobalScope.launch {
      repeatForever {
        runBlocking(Dispatchers.IO) {
          val jobsToLaunch = CONCURRENCY - runningJobs
          (1..jobsToLaunch).mapNotNull { queue.poll() }
            .forEach { execution ->
              if (execution.isTimeToExecute()) {
                queue.add(execution)
                return@forEach
              }
              launch {
                runningJobs++
                logger.debug("Running jobs: $runningJobs")
                executeInNewTransaction(transactionManager, isolationLevel = TransactionDefinition.ISOLATION_DEFAULT) {
                  val lockedExecution = getItemIfCanAcquireLock(execution.chunkExecutionId)
                    ?: let {
                      logger.debug("⚠️ Chunk ${execution.chunkExecutionId} is locked, skipping")
                      return@executeInNewTransaction
                    }
                  publishRemoveFromQueue()
                  logger.debug("Job ${lockedExecution.batchJob.id}: 🟡 Processing chunk ${lockedExecution.id}")
                  ChunkProcessingUtil(lockedExecution, applicationContext).processChunk()
                  logger.debug("Job ${lockedExecution.batchJob.id}: ✅ Processed chunk ${lockedExecution.id}")
                }
              }.invokeOnCompletion {
                logger.debug("Job ${execution.executeAfter}: Completed")
                runningJobs--
                logger.debug("Running jobs: $runningJobs")
              }
            }
        }
      }
    }
  }

  private fun publishRemoveFromQueue() {
  }

  fun ExecutionQueueItem.isTimeToExecute(): Boolean {
    val executeAfter = this.executeAfter ?: return true
    return executeAfter < currentDateProvider.date.time
  }

  @PreDestroy
  fun stop() {
    run = false
    runBlocking(Dispatchers.IO) {
      runJob?.join()
    }
  }

  @EventListener
  fun repeatForever(fn: () -> Unit) {
    while (run) {
      try {
        fn()
      } catch (e: Throwable) {
        Sentry.captureException(e)
        logger.error("Error in batch job action service", e)
      }
    }
  }

  @Scheduled(fixedDelay = 60000)
  fun populateQueue() {
    val data = entityManager.createQuery(
      """
          from BatchJobChunkExecution bjce
          join fetch bjce.batchJob
          where bjce.status = :status
          order by bjce.createdAt asc, bjce.executeAfter asc, bjce.id asc
      """.trimIndent(),
      BatchJobChunkExecution::class.java
    )
      .setParameter("status", BatchJobChunkExecutionStatus.PENDING)
      .setHint(
        "javax.persistence.lock.timeout",
        LockOptions.SKIP_LOCKED
      ).resultList
    queue.clear()
    data.forEach { addToQueue(it) }
  }

  fun addToQueue(execution: BatchJobChunkExecution) {
    queue.add(ExecutionQueueItem(execution.id, execution.executeAfter?.time))
  }

  fun getItemIfCanAcquireLock(id: Long): BatchJobChunkExecution? {
    return entityManager.createQuery(
      """
         from BatchJobChunkExecution bjce
         where bjce.id = :id
      """.trimIndent(),
      BatchJobChunkExecution::class.java
    )
      .setParameter("id", id)
      .setLockMode(LockModeType.PESSIMISTIC_WRITE)
      .setHint(
        "javax.persistence.lock.timeout",
        LockOptions.SKIP_LOCKED
      ).resultList.singleOrNull()
  }
}
