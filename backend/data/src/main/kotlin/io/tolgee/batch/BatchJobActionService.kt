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
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import java.util.concurrent.ConcurrentHashMap
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
  private val usingRedisProvider: UsingRedisProvider,
  private val progressManager: ProgressManager,
  @Lazy
  private val batchJobService: BatchJobService,
) : Logging {
  companion object {
    const val MIN_TIME_BETWEEN_OPERATIONS = 10
    const val CONCURRENCY = 10
  }

  var runJob: Job? = null
  var run = true

  var queue = ConcurrentLinkedQueue<ExecutionQueueItem>()
  var runningJobs: ConcurrentHashMap<Long, Pair<Long, Job>> = ConcurrentHashMap()
  var pause = false

  @EventListener(ApplicationReadyEvent::class)
  fun run() {
    executeInNewTransaction(transactionManager) {
      populateQueue()
    }

    @Suppress("OPT_IN_USAGE")
    runJob = GlobalScope.launch {
      runBlocking(Dispatchers.IO) {
        repeatForever {
          if (pause) {
            return@repeatForever
          }
          val jobsToLaunch = CONCURRENCY - runningJobs.size
          if (jobsToLaunch <= 0) {
            return@repeatForever
          }

          logger.trace("Jobs to launch: $jobsToLaunch")
          (1..jobsToLaunch).mapNotNull { queue.poll() }
            .forEach { executionItem ->
              if (!executionItem.isTimeToExecute()) {
                logger.debug(
                  """Execution ${executionItem.chunkExecutionId} not ready to execute, adding back to queue:
                  | Difference ${executionItem.executeAfter!! - currentDateProvider.date.time}""".trimMargin()
                )
                queue.add(executionItem)
                return@forEach
              }
              val runningJobsNow = runningJobs.size

              val job = launch {
                try {
                  var retryExecution: BatchJobChunkExecution? = null
                  val execution = executeInNewTransaction(
                    transactionManager,
                    isolationLevel = TransactionDefinition.ISOLATION_DEFAULT
                  ) {
                    val lockedExecution = getItemIfCanAcquireLock(executionItem.chunkExecutionId)
                      ?: let {
                        logger.debug("⚠️ Chunk ${executionItem.chunkExecutionId} is locked, skipping")
                        return@executeInNewTransaction null
                      }
                    if (lockedExecution.status != BatchJobChunkExecutionStatus.PENDING) {
                      logger.debug("⚠️ Chunk ${executionItem.chunkExecutionId} is not pending, skipping")
                      return@executeInNewTransaction null
                    }
                    publishRemoveConsuming(executionItem)
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
                  execution?.let { progressManager.handleChunkCompletedCommitted(it) }
                  retryExecution?.let {
                    addToQueue(it)
                    logger.debug("Job ${it.batchJob.id}: Added chunk ${it.id} for re-trial")
                  }
                } catch (e: Throwable) {
                  logger.error("Error processing chunk ${executionItem.chunkExecutionId}", e)
                  Sentry.captureException(e)
                  queue.add(executionItem)
                }
              }
              runningJobs[executionItem.chunkExecutionId] = executionItem.jobId to job
              job.invokeOnCompletion {
                runningJobs.remove(executionItem.chunkExecutionId)
                logger.debug("Chunk ${executionItem.chunkExecutionId}: Completed")
                logger.debug("Running jobs: ${runningJobs.size}")
//                if (job.isCancelled) {
//                  executeInNewTransaction(transactionManager) {
//                    val execution =
//                      getItemIfCanAcquireLock(executionItem.chunkExecutionId) ?: return@executeInNewTransaction
//                    execution.status = BatchJobChunkExecutionStatus.CANCELLED
//                    entityManager.persist(execution)
//                    progressManager.handleProgress(execution)
//                  }
//                }
              }
              logger.debug("Execution ${executionItem.chunkExecutionId} launched. Running jobs: $runningJobsNow")
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
                        join fetch bjce.batchJob bk
                        where bjce.status = :executionStatus
                        order by bjce.createdAt asc, bjce.executeAfter asc, bjce.id asc
      """.trimIndent(),
      BatchJobChunkExecution::class.java
    ).setParameter("executionStatus", BatchJobChunkExecutionStatus.PENDING)
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
    ExecutionQueueItem(id, batchJob.id, executeAfter?.time)

  fun getItemIfCanAcquireLock(id: Long): BatchJobChunkExecution? {
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
    queue.removeIf { it.jobId == jobId }
    runningJobs.filter { it.value.first == jobId }.forEach {
      it.value.second.cancel()
    }
  }
}
