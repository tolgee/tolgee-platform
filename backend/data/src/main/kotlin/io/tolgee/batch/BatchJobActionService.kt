package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.sentry.Sentry
import io.tolgee.Metrics
import io.tolgee.activity.ActivityHolder
import io.tolgee.batch.data.BatchJobDto
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
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.hibernate.LockOptions
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.UnexpectedRollbackException
import java.util.concurrent.CancellationException
import kotlin.coroutines.coroutineContext

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
  private val savePointManager: SavePointManager,
  private val currentDateProvider: CurrentDateProvider,
  private val activityHolder: ActivityHolder,
  private val metrics: Metrics,
) : Logging {
  @WithSpan
  suspend fun handleItem(
    executionItem: ExecutionQueueItem,
    batchJobDto: BatchJobDto,
  ) {
    val span = Span.current()
    span.setAttribute("batch.job.id", batchJobDto.id.toString())
    span.setAttribute("batch.chunk.execution.id", executionItem.chunkExecutionId.toString())
    batchJobDto.projectId?.let { span.setAttribute("project.id", it.toString()) }

    val coroutineContext = coroutineContext
    var retryExecution: BatchJobChunkExecution? = null
    try {
      val execution =
        catchingExceptions(executionItem) {
          executeInNewTransaction(transactionManager) { transactionStatus ->

            val lockedExecution =
              getPendingUnlockedExecutionItem(executionItem)
                ?: return@executeInNewTransaction null

            publishRemoveConsuming(executionItem)

            progressManager.handleJobRunning(batchJobDto)

            logger.debug("Job ${batchJobDto.id}: 🟡 Processing chunk ${lockedExecution.id}")
            val savepoint = savePointManager.setSavepoint()
            val util = ChunkProcessingUtil(lockedExecution, applicationContext, coroutineContext)
            util.processChunk()

            if (transactionStatus.isRollbackOnly) {
              logger.debug("Job ${batchJobDto.id}: 🛑 Rollbacking chunk ${lockedExecution.id}")
              savePointManager.rollbackSavepoint(savepoint)
              // we have rolled back the transaction, so no targets were actually successfull
              lockedExecution.successTargets = listOf()
              entityManager.clear()
              rollbackActivity()
            }

            progressManager.handleProgress(lockedExecution, batchJobDto)
            entityManager.persist(entityManager.merge(lockedExecution))

            if (lockedExecution.retry) {
              retryExecution = util.retryExecution
              entityManager.persist(util.retryExecution)
              entityManager.flush()
            }

            logger.debug("Job ${batchJobDto.id}: ✅ Processed chunk ${lockedExecution.id}")
            return@executeInNewTransaction lockedExecution
          }
        }
      execution?.let {
        logger.debug("Job: ${it.batchJob.id} - Handling execution committed ${it.id} (standard flow)")
        progressManager.handleChunkCompletedCommitted(it, batchJobDto = batchJobDto)
      }
      addRetryExecutionToQueue(retryExecution, jobCharacter = executionItem.jobCharacter)
    } catch (e: Throwable) {
      span.recordException(e)
      span.setStatus(StatusCode.ERROR)
      when (e) {
        is CancellationException, is kotlinx.coroutines.CancellationException -> {
          // Coroutine was cancelled (e.g., during job cancellation)
          // Update the execution status to CANCELLED in a new transaction
          logger.debug(
            "Job ${executionItem.jobId}: ⚠️ Chunk ${executionItem.chunkExecutionId} was cancelled",
          )
          handleCancelledExecution(executionItem)
        }

        is UnexpectedRollbackException -> {
          logger.debug(
            "Job ${executionItem.jobId}: ⚠️ Chunk ${executionItem.chunkExecutionId}" +
              " thrown UnexpectedRollbackException",
          )
        }

        else -> {
          logger.error("Job ${executionItem.jobId}: ⚠️ Chunk ${executionItem.chunkExecutionId} thrown error", e)
          Sentry.captureException(e)
        }
      }
    }
  }

  private fun handleCancelledExecution(executionItem: ExecutionQueueItem) {
    try {
      val execution =
        executeInNewTransaction(transactionManager) {
          val exec = entityManager.find(BatchJobChunkExecution::class.java, executionItem.chunkExecutionId)
          if (exec != null && !exec.status.completed) {
            exec.status = BatchJobChunkExecutionStatus.CANCELLED
            entityManager.persist(exec)
            progressManager.handleProgress(exec)
            exec
          } else {
            null
          }
        }
      execution?.let {
        progressManager.handleChunkCompletedCommitted(it)
      }
    } catch (e: Exception) {
      logger.warn("Failed to handle cancelled execution ${executionItem.chunkExecutionId}", e)
    }
  }

  private fun rollbackActivity() {
    activityHolder.modifiedEntities.clear()
    activityHolder.activityRevision.describingRelations.clear()
  }

  private fun getPendingUnlockedExecutionItem(executionItem: ExecutionQueueItem): BatchJobChunkExecution? {
    val lockedExecution = getExecutionIfCanAcquireLockInDb(executionItem.chunkExecutionId)

    if (lockedExecution == null) {
      metrics.batchJobManagementItemAlreadyLockedCounter.increment()
      logger.debug("⚠️ Chunk ${executionItem.chunkExecutionId} (job: ${executionItem.jobId}) is locked, skipping")
      progressManager.finalizeIfCompleted(executionItem.jobId)
      return null
    }
    if (lockedExecution.status != BatchJobChunkExecutionStatus.PENDING) {
      logger.debug("⚠️ Chunk ${executionItem.chunkExecutionId} (job: ${executionItem.jobId}) is not pending, skipping")
      progressManager.finalizeIfCompleted(executionItem.jobId)
      return null
    }

    return lockedExecution
  }

  private fun addRetryExecutionToQueue(
    retryExecution: BatchJobChunkExecution?,
    jobCharacter: JobCharacter,
  ) {
    retryExecution?.let {
      batchJobChunkExecutionQueue.addToQueue(it, jobCharacter)
      logger.debug("Job ${it.batchJob.id}: Added chunk ${it.id} for re-trial")
    }
  }

  private inline fun <reified T> catchingExceptions(
    executionItem: ExecutionQueueItem,
    fn: () -> T,
  ): T? {
    return try {
      fn()
    } catch (e: Throwable) {
      logger.error("Error processing chunk ${executionItem.chunkExecutionId}", e)
      Sentry.captureException(e)
      val maxRetries = 10
      if (++executionItem.managementErrorRetrials > maxRetries) {
        logger.error("Chunk ${executionItem.chunkExecutionId} failed $maxRetries times, failing...")
        failExecution(executionItem.chunkExecutionId, e)
        metrics.batchJobManagementTotalFailureFailedCounter.increment()
        return null
      }
      metrics.batchJobManagementFailureWithRetryCounter.increment()
      executionItem.executeAfter = currentDateProvider.date.addSeconds(2).time
      batchJobChunkExecutionQueue.addItemsToQueue(listOf(executionItem))
      null
    }
  }

  private fun failExecution(
    chunkExecutionId: Long,
    e: Throwable,
  ) {
    val execution =
      executeInNewTransaction(transactionManager) {
        val execution = entityManager.find(BatchJobChunkExecution::class.java, chunkExecutionId)
        execution.status = BatchJobChunkExecutionStatus.FAILED
        execution.errorMessage = Message.EXECUTION_FAILED_ON_MANAGEMENT_ERROR
        execution.stackTrace = e.stackTraceToString()
        execution.errorKey = "management_error"
        entityManager.persist(execution)
        execution
      }
    executeInNewTransaction(transactionManager) {
      progressManager.handleProgress(execution)
    }
    progressManager.handleChunkCompletedCommitted(execution)
  }

  fun publishRemoveConsuming(item: ExecutionQueueItem) {
    if (usingRedisProvider.areWeUsingRedis) {
      val message =
        jacksonObjectMapper()
          .writeValueAsString(JobQueueItemsEvent(listOf(item), QueueEventType.REMOVE))
      redisTemplate.convertAndSend(RedisPubSubReceiverConfiguration.JOB_QUEUE_TOPIC, message)
    }
  }

  @WithSpan
  private fun getExecutionIfCanAcquireLockInDb(id: Long): BatchJobChunkExecution? {
    entityManager.createNativeQuery("""SET enable_seqscan=off""")
    return entityManager
      .createQuery(
        """
        from BatchJobChunkExecution bjce
        where bjce.id = :id
        """.trimIndent(),
        BatchJobChunkExecution::class.java,
      ).setParameter("id", id)
      .setLockMode(LockModeType.PESSIMISTIC_WRITE)
      .setHint(
        "jakarta.persistence.lock.timeout",
        LockOptions.SKIP_LOCKED,
      ).resultList
      .singleOrNull()
  }
}
