package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.sentry.Sentry
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.UsingRedisProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration
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
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PreDestroy
import javax.persistence.EntityManager
import javax.persistence.LockModeType

@Service
class BatchJobActionService(
  private val entityManager: EntityManager,
  private val currentDateProvider: CurrentDateProvider,
  private val transactionManager: PlatformTransactionManager,
  private val applicationContext: ApplicationContext,
  private val usingRedisProvider: UsingRedisProvider,
  private val progressManager: ProgressManager
) : Logging {
  companion object {
    const val MIN_TIME_BETWEEN_OPERATIONS = 100
    const val CONCURRENCY = 10
  }

  var runJob: Job? = null
  var run = true

  var queue = ConcurrentLinkedQueue<ExecutionQueueItem>()
  var runningJobs: AtomicInteger = AtomicInteger(0)

  @EventListener(ApplicationReadyEvent::class)
  fun run() {
    executeInNewTransaction(transactionManager) {
      populateQueue()
    }

    @Suppress("OPT_IN_USAGE")
    runJob = GlobalScope.launch {
      runBlocking(Dispatchers.IO) {
        repeatForever {
          val jobsToLaunch = CONCURRENCY - runningJobs.get()
          if (jobsToLaunch <= 0) {
            return@repeatForever
          }

          logger.debug("Jobs to launch: $jobsToLaunch")
          (1..jobsToLaunch).mapNotNull { queue.poll() }
            .forEach { executionItem ->
              if (!executionItem.isTimeToExecute()) {
                logger.debug("""Execution ${executionItem.chunkExecutionId} not ready to execute, adding back to queue: Difference ${executionItem.executeAfter!! - currentDateProvider.date.time}""")
                queue.add(executionItem)
                return@forEach
              }
              var runningJobsNow = runningJobs.incrementAndGet()
              logger.debug("Execution ${executionItem.chunkExecutionId} launched. Running jobs: $runningJobsNow")
              launch {
                try {
                  var retryExecution: BatchJobChunkExecution? = null
                  executeInNewTransaction(
                    transactionManager,
                    isolationLevel = TransactionDefinition.ISOLATION_DEFAULT
                  ) {
                    val lockedExecution = getItemIfCanAcquireLock(executionItem.chunkExecutionId)
                      ?: let {
                        logger.debug("⚠️ Chunk ${executionItem.chunkExecutionId} is locked, skipping")
                        return@executeInNewTransaction
                      }
                    if (lockedExecution.status != BatchJobChunkExecutionStatus.PENDING) {
                      logger.debug("⚠️ Chunk ${executionItem.chunkExecutionId} is not pending, skipping")
                      return@executeInNewTransaction
                    }
                    publishRemoveConsuming(executionItem)
                    logger.debug("Job ${lockedExecution.batchJob.id}: 🟡 Processing chunk ${lockedExecution.id}")
                    val util = ChunkProcessingUtil(lockedExecution, applicationContext)
                    util.processChunk()
                    progressManager.handleProgress(lockedExecution)
                    entityManager.persist(lockedExecution)
                    if (lockedExecution.retry) {
                      retryExecution = util.retryExecution
                    }
                    logger.debug("Job ${lockedExecution.batchJob.id}: ✅ Processed chunk ${lockedExecution.id}")
                  }
                  retryExecution?.let {
                    executeInNewTransaction(transactionManager) {
                      entityManager.persist(it)
                    }
                    addToQueue(it)
                    logger.debug("Job ${it.batchJob.id}: Added chunk ${it.id} for re-trial")
                  }
                } catch (e: Throwable) {
                  logger.error("Error processing chunk ${executionItem.chunkExecutionId}", e)
                  Sentry.captureException(e)
                }
              }.invokeOnCompletion {
                logger.debug("Chunk ${executionItem.chunkExecutionId}: Completed")
                runningJobsNow = runningJobs.decrementAndGet()
                logger.debug("Running jobs: $runningJobsNow")
              }
            }
        }
      }
    }
  }

  fun publishRemoveConsuming(item: ExecutionQueueItem) {
    if (usingRedisProvider.areWeUsingRedis) {
      val message = jacksonObjectMapper().writeValueAsString(JobQueueItemEvent(item, QueueItemType.REMOVE))
      redisTemplate.convertAndSend(RedisPubSubReceiverConfiguration.JOB_QUEUE_TOPIC, message)
    }
  }

  fun ExecutionQueueItem.isTimeToExecute(): Boolean {
    val executeAfter = this.executeAfter ?: return true
    return executeAfter <= currentDateProvider.date.time
  }

  val redisTemplate: StringRedisTemplate by lazy { applicationContext.getBean(StringRedisTemplate::class.java) }

  @EventListener(JobQueueItemEvent::class)
  fun onJobItemEvent(event: JobQueueItemEvent) {
    when (event.type) {
      QueueItemType.ADD -> queue.add(event.item)
      QueueItemType.REMOVE -> queue.remove(event.item)
    }
  }

  @PreDestroy
  fun stop() {
    run = false
    runBlocking(Dispatchers.IO) {
      runJob?.join()
    }
  }

  fun repeatForever(fn: () -> Unit) {
    while (run) {
      try {
        val startTime = System.currentTimeMillis()
        fn()
        val sleepTime = MIN_TIME_BETWEEN_OPERATIONS - (System.currentTimeMillis() - startTime)
        if (sleepTime > 0) {
          Thread.sleep(sleepTime)
        }
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
    data.forEach { queue.add(it.toItem()) }
  }

  fun addToQueue(execution: BatchJobChunkExecution) {
    val item = execution.toItem()
    if (usingRedisProvider.areWeUsingRedis) {
      val event = JobQueueItemEvent(item, QueueItemType.ADD)
      redisTemplate.convertAndSend(
        RedisPubSubReceiverConfiguration.JOB_QUEUE_TOPIC,
        jacksonObjectMapper().writeValueAsString(event)
      )
      return
    }
    queue.add(item)
  }

  private fun BatchJobChunkExecution.toItem() =
    ExecutionQueueItem(id, executeAfter?.time)

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
