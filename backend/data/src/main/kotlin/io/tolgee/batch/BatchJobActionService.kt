package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.sentry.Sentry
import io.tolgee.activity.ActivityHolder
import io.tolgee.batch.data.ExecutionQueueItem
import io.tolgee.batch.data.QueueEventType
import io.tolgee.batch.events.JobQueueItemsEvent
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.SavePointManager
import io.tolgee.component.UsingRedisProvider
import io.tolgee.constants.Message
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration
import io.tolgee.util.Logging
import io.tolgee.util.addSeconds
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
import org.springframework.transaction.UnexpectedRollbackException
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
  private val concurrentExecutionLauncher: BatchJobConcurrentLauncher,
  private val savePointManager: SavePointManager,
  private val currentDateProvider: CurrentDateProvider,
  private val activityHolder: ActivityHolder,
  private val batchJobProjectLockingManager: BatchJobProjectLockingManager
) : Logging {
  companion object {
    const val MIN_TIME_BETWEEN_OPERATIONS = 100
  }

  @EventListener(ApplicationReadyEvent::class)
  fun run() {
    println("Application ready")
    executeInNewTransaction(transactionManager) {
      batchJobChunkExecutionQueue.populateQueue()
    }

    concurrentExecutionLauncher.run { executionItem, coroutineContext ->
      var retryExecution: BatchJobChunkExecution? = null
      try {
        val execution = catchingExceptions(executionItem) {
          executeInNewTransaction(
            transactionManager,
            isolationLevel = TransactionDefinition.ISOLATION_DEFAULT
          ) { transactionStatus ->
            val lockedExecution = getPendingUnlockedExecutionItem(executionItem)
              ?: return@executeInNewTransaction null

            publishRemoveConsuming(executionItem)

            progressManager.handleJobRunning(lockedExecution.batchJob.id)
            val batchJobDto = batchJobService.getJobDto(lockedExecution.batchJob.id)

            logger.debug("Job ${batchJobDto.id}: 🟡 Processing chunk ${lockedExecution.id}")
            val savepoint = savePointManager.setSavepoint()
            val util = ChunkProcessingUtil(lockedExecution, applicationContext, coroutineContext)
            util.processChunk()

            if (transactionStatus.isRollbackOnly) {
              logger.debug("Job ${batchJobDto.id}: 🛑 Rollbacking chunk ${lockedExecution.id}")
              savePointManager.rollbackSavepoint(savepoint)
              // we have rolled back the transaction, so no targets were actually successfull
              lockedExecution.successTargets = listOf()
              rollbackActivity()
            }

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
        addRetryExecutionToQueue(retryExecution, jobCharacter = executionItem.jobCharacter)
      } catch (e: Throwable) {
        progressManager.rollbackSetToRunning(executionItem.chunkExecutionId, executionItem.jobId)
        when (e) {
          is UnexpectedRollbackException -> {
            logger.debug(
              "Job ${executionItem.jobId}: ⚠️ Chunk ${executionItem.chunkExecutionId}" +
                " thrown UnexpectedRollbackException"
            )
          }

          else -> {
            logger.error("Job ${executionItem.jobId}: ⚠️ Chunk ${executionItem.chunkExecutionId} thrown error", e)
            Sentry.captureException(e)
          }
        }
      }
    }
  }

  private fun rollbackActivity() {
    activityHolder.modifiedEntities.clear()
    activityHolder.activityRevision.describingRelations.clear()
  }

  private fun getPendingUnlockedExecutionItem(executionItem: ExecutionQueueItem): BatchJobChunkExecution? {
    val lockedExecution = getExecutionIfCanAcquireLockInDb(executionItem.chunkExecutionId)

    if (lockedExecution == null) {
      logger.debug("⚠️ Chunk ${executionItem.chunkExecutionId} (job: ${executionItem.jobId}) is locked, skipping")
      progressManager.rollbackSetToRunning(executionItem.chunkExecutionId, executionItem.jobId)
      batchJobProjectLockingManager.unlockJobIfCompleted(executionItem.jobId)
      return null
    }
    if (lockedExecution.status != BatchJobChunkExecutionStatus.PENDING) {
      logger.debug("⚠️ Chunk ${executionItem.chunkExecutionId} (job: ${executionItem.jobId}) is not pending, skipping")
      progressManager.rollbackSetToRunning(executionItem.chunkExecutionId, executionItem.jobId)
      batchJobProjectLockingManager.unlockJobIfCompleted(executionItem.jobId)

      return null
    }

    return lockedExecution
  }

  private fun addRetryExecutionToQueue(retryExecution: BatchJobChunkExecution?, jobCharacter: JobCharacter) {
    retryExecution?.let {
      batchJobChunkExecutionQueue.addToQueue(it, jobCharacter)
      logger.debug("Job ${it.batchJob.id}: Added chunk ${it.id} for re-trial")
    }
  }

  private inline fun <reified T> catchingExceptions(executionItem: ExecutionQueueItem, fn: () -> T): T? {
    return try {
      fn()
    } catch (e: Throwable) {
      logger.error("Error processing chunk ${executionItem.chunkExecutionId}", e)
      Sentry.captureException(e, "Processing of chunk unexpectedly failed ${executionItem.chunkExecutionId}")
      val maxRetries = 3
      if (++executionItem.managementErrorRetrials > maxRetries) {
        logger.error("Chunk ${executionItem.chunkExecutionId} failed $maxRetries times, failing...")
        failExecution(executionItem.chunkExecutionId, e)
        return null
      }
      executionItem.executeAfter = currentDateProvider.date.addSeconds(2).time
      batchJobChunkExecutionQueue.addItemsToQueue(listOf(executionItem))
      null
    }
  }

  private fun failExecution(chunkExecutionId: Long, e: Throwable) {
    val execution = executeInNewTransaction(transactionManager) {
      val execution = entityManager.find(BatchJobChunkExecution::class.java, chunkExecutionId)
      execution.status = BatchJobChunkExecutionStatus.FAILED
      execution.errorMessage = Message.EXECUTION_FAILED_ON_MANAGEMENT_ERROR
      execution.stackTrace = e.stackTraceToString()
      execution.errorKey = "management_error"
      entityManager.persist(execution)
      progressManager.handleProgress(execution)
      execution
    }
    progressManager.handleChunkCompletedCommitted(execution)
  }

  fun publishRemoveConsuming(item: ExecutionQueueItem) {
    if (usingRedisProvider.areWeUsingRedis) {
      val message = jacksonObjectMapper()
        .writeValueAsString(JobQueueItemsEvent(listOf(item), QueueEventType.REMOVE))
      redisTemplate.convertAndSend(RedisPubSubReceiverConfiguration.JOB_QUEUE_TOPIC, message)
    }
  }

  private fun getExecutionIfCanAcquireLockInDb(id: Long): BatchJobChunkExecution? {
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
    concurrentExecutionLauncher.runningJobs.filter { it.value.first.id == jobId }.forEach {
      it.value.second.cancel()
    }
  }
}
