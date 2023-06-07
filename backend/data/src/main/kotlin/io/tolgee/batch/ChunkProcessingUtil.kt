package io.tolgee.batch

import io.sentry.Sentry
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.BatchJobChunkMessageBody
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.util.executeInNewTransaction
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import javax.persistence.EntityManager
import kotlin.math.pow

open class ChunkProcessingUtil(val message: Message, val applicationContext: ApplicationContext) {
  open fun processChunk() {
    batchJobChunkMessage.retries++
    try {
      batchJobService.getProcessor(job.type).process(job, toProcess)
      execution.status = BatchJobChunkExecutionStatus.SUCCESS
    } catch (e: Throwable) {
      handleException(e)
    } finally {
      executeInNewTransaction(transactionManager) {
        execution.successTargets = successfulTargets.toList()
        entityManager.persist(execution)
      }
    }
  }

  private fun handleException(exception: Throwable) {
    execution.exception = exception.stackTraceToString()

    if (exception is ChunkFailedException) {
      execution.successTargets = successfulTargets.toList()
    }

    if (exception is FailedDontRequeueException) {
      execution.status = BatchJobChunkExecutionStatus.FAILED
      return
    }

    if (batchJobChunkMessage.retries >= job.type.maxRetries) {
      Sentry.captureException(exception)
      execution.status = BatchJobChunkExecutionStatus.FAILED
      return
    }

    batchJobChunkMessage.waitUntil = currentDateProvider.date.time + job.type.defaultRetryTimeoutInMs

    if (exception is RequeueWithTimeoutException) {
      if (batchJobChunkMessage.retries >= exception.maxRetries) {
        Sentry.captureException(exception)
        execution.status = BatchJobChunkExecutionStatus.FAILED
        return
      }
      val timeout = getTimeout(exception)
      batchJobChunkMessage.waitUntil = timeout + currentDateProvider.date.time
    }

    execution.status = BatchJobChunkExecutionStatus.RETRYING
    rabbitTemplate.convertAndSend(
      "batch-operations-wait-queue", batchJobService.convertMessage(batchJobChunkMessage)
    )
  }

  private fun getTimeout(exception: RequeueWithTimeoutException) =
    exception.timeoutInMs * (exception.increaseFactor.toDouble().pow(batchJobChunkMessage.retries.toDouble())).toLong()

  private val batchJobChunkMessage by lazy { batchJobService.parseMessage(message) }

  private val job by lazy { batchJobService.getJob(batchJobChunkMessage.batchJobId) }

  private val execution by lazy {
    BatchJobChunkExecution().apply {
      this.batchJob = job
      this.chunkNumber = batchJobChunkMessage.chunkNumber
    }
  }

  private val entityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }

  private val rabbitTemplate by lazy {
    applicationContext.getBean(RabbitTemplate::class.java)
  }

  private val transactionManager by lazy {
    applicationContext.getBean(PlatformTransactionManager::class.java)
  }

  private val currentDateProvider by lazy {
    applicationContext.getBean(CurrentDateProvider::class.java)
  }

  private val batchJobService by lazy {
    applicationContext.getBean(BatchJobService::class.java)
  }

  private val successfulTargets by lazy { mutableSetOf<Long>() }

  private val toProcess by lazy {
    val chunked = job.target.chunked(job.chunkSize)
    val chunk = chunked[batchJobChunkMessage.chunkNumber]
    val previousExecutions = getPreviousChunkExecutions(batchJobChunkMessage)
    val previousSuccessfulTargets = previousExecutions.flatMap { it.successTargets }.toSet()
    val toProcess = chunk.toMutableSet()
    toProcess.removeAll(previousSuccessfulTargets)
    toProcess.toList()
  }

  private fun getPreviousChunkExecutions(message: BatchJobChunkMessageBody): List<BatchJobChunkExecution> {
    return entityManager.createQuery(
      """
      from BatchJobChunkExecution 
      where chunkNumber = :chunkNumber 
          and batchJob.id = :batchJobId
      """.trimIndent()
    )
      .setParameter("chunkNumber", message.chunkNumber)
      .setParameter("batchJobId", message.batchJobId)
      .resultList as List<BatchJobChunkExecution>
  }

  @Component
  class Factory {
    operator fun invoke(message: Message, applicationContext: ApplicationContext): ChunkProcessingUtil {
      return ChunkProcessingUtil(message, applicationContext)
    }
  }
}
