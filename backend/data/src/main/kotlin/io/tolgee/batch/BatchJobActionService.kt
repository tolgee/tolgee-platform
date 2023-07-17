package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.sentry.Sentry
import io.tolgee.component.UsingRedisProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.hibernate.LockOptions
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import javax.persistence.EntityManager
import javax.persistence.LockModeType

@Service
class BatchJobActionService(
  private val entityManager: EntityManager,
  private val transactionManager: PlatformTransactionManager,
  private val applicationContext: ApplicationContext,
  private val usingRedisProvider: UsingRedisProvider,
  @Lazy
  private val progressManager: ProgressManager,
  @Lazy
  private val batchJobService: BatchJobService,
  private val batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue,
  @Lazy
  private val redisTemplate: StringRedisTemplate,
  private val concurrentExecutionLauncher: BatchJobConcurrentLauncher
) : Logging {
  companion object {
    const val MIN_TIME_BETWEEN_OPERATIONS = 10
  }

  @EventListener(ApplicationReadyEvent::class)
  fun run() {
    println("Application ready")
    executeInNewTransaction(transactionManager) {
      batchJobChunkExecutionQueue.populateQueue()
    }

    concurrentExecutionLauncher.run { executionItem, coroutineContext ->
      var retryExecution: BatchJobChunkExecution? = null
      val execution = executeInNewTransaction(
        transactionManager,
        isolationLevel = TransactionDefinition.ISOLATION_DEFAULT
      ) {
        catchingExceptions(executionItem) {

          val lockedExecution = getPendingUnlockedExecutionItem(executionItem)
            ?: return@executeInNewTransaction null

          publishRemoveConsuming(executionItem)

          progressManager.handleJobRunning(lockedExecution.batchJob.id)
          val batchJobDto = batchJobService.getJobDto(lockedExecution.batchJob.id)

          logger.debug("Job ${batchJobDto.id}: 🟡 Processing chunk ${lockedExecution.id}")
          val util = ChunkProcessingUtil(lockedExecution, applicationContext, coroutineContext)
          util.processChunk()

          progressManager.handleProgress(lockedExecution)
          entityManager.persist(lockedExecution)

          if (lockedExecution.retry) {
            retryExecution = util.retryExecution
            entityManager.persist(util.retryExecution)
          }

          logger.debug("Job ${batchJobDto.id}: ✅ Processed chunk ${lockedExecution.id}")
          return@executeInNewTransaction lockedExecution
        }
      }
      execution?.let { progressManager.handleChunkCompletedCommitted(it) }
      addRetryExecutionToQueue(retryExecution)
    }
  }

  private fun getPendingUnlockedExecutionItem(executionItem: ExecutionQueueItem): BatchJobChunkExecution? {
    val lockedExecution = getExecutionIfCanAcquireLock(executionItem.chunkExecutionId)
    if (lockedExecution == null) {
      logger.debug("⚠️ Chunk ${executionItem.chunkExecutionId} is locked, skipping")
      return null
    }
    if (lockedExecution.status != BatchJobChunkExecutionStatus.PENDING) {
      logger.debug("⚠️ Chunk ${executionItem.chunkExecutionId} is not pending, skipping")
      return null
    }
    return lockedExecution
  }

  private fun addRetryExecutionToQueue(retryExecution: BatchJobChunkExecution?) {
    retryExecution?.let {
      batchJobChunkExecutionQueue.addToQueue(listOf(it))
      logger.debug("Job ${it.batchJob.id}: Added chunk ${it.id} for re-trial")
    }
  }

  private inline fun <reified T> catchingExceptions(executionItem: ExecutionQueueItem, fn: () -> T): T? {
    return try {
      fn()
    } catch (e: Throwable) {
      logger.error("Error processing chunk ${executionItem.chunkExecutionId}", e)
      Sentry.captureException(e)
      batchJobChunkExecutionQueue.addItemsToLocalQueue(listOf(executionItem))
      null
    }
  }

  fun publishRemoveConsuming(item: ExecutionQueueItem) {
    if (usingRedisProvider.areWeUsingRedis) {
      val message = jacksonObjectMapper()
        .writeValueAsString(JobQueueItemsEvent(listOf(item), QueueEventType.REMOVE))
      redisTemplate.convertAndSend(RedisPubSubReceiverConfiguration.JOB_QUEUE_TOPIC, message)
    }
  }

  fun getExecutionIfCanAcquireLock(id: Long): BatchJobChunkExecution? {
    entityManager.createNativeQuery("""SET enable_seqscan=off""")
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

  fun cancelLocalJob(jobId: Long) {
    batchJobChunkExecutionQueue.cancelJob(jobId)
    concurrentExecutionLauncher.runningJobs.filter { it.value.first == jobId }.forEach {
      it.value.second.cancel()
    }
  }
}
