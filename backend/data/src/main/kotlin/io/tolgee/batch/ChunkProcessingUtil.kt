package io.tolgee.batch

import io.sentry.Sentry
import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import org.hibernate.LockOptions
import org.springframework.context.ApplicationContext
import org.springframework.transaction.PlatformTransactionManager
import java.util.*
import javax.persistence.EntityManager
import kotlin.math.pow

open class ChunkProcessingUtil(val execution: BatchJobChunkExecution, val applicationContext: ApplicationContext) {
  open fun processChunk() {
    try {
      batchJobService.getProcessor<Any>(job.type).process(job, toProcess)
      execution.status = BatchJobChunkExecutionStatus.SUCCESS
    } catch (e: Throwable) {
      handleException(e)
    } finally {
      execution.successTargets = successfulTargets.toList()
    }
  }

  private fun handleException(exception: Throwable) {
    execution.exception = exception.stackTraceToString()
    execution.status = BatchJobChunkExecutionStatus.FAILED
    Sentry.captureException(exception)

    if (exception is ChunkFailedException) {
      execution.successTargets = successfulTargets.toList()
    }

    if (exception is FailedDontRequeueException || retries >= job.type.maxRetries) {
      return
    }

    retryFailedExecution(exception)
  }

  private fun retryFailedExecution(exception: Throwable) {
    retryExecution.executeAfter = Date(currentDateProvider.date.time + job.type.defaultRetryTimeoutInMs)

    if (exception is RequeueWithTimeoutException) {
      if (retries >= exception.maxRetries) {
        Sentry.captureException(exception)
        setJobFailed()
        return
      }
      val timeout = getTimeout(exception)
      retryExecution.executeAfter = Date(timeout + currentDateProvider.date.time)
    }
  }

  private fun setJobFailed() {
    job.status = BatchJobStatus.FAILED
    entityManager.persist(job)
  }

  private fun getTimeout(exception: RequeueWithTimeoutException) =
    exception.timeoutInMs * (exception.increaseFactor.toDouble().pow(retries.toDouble())).toLong()

  private val job by lazy { execution.batchJob }

  private val entityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
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
    val chunk = chunked[execution.chunkNumber]
    val previousSuccessfulTargets = previousExecutions.flatMap { it.successTargets }.toSet()
    val toProcess = chunk.toMutableSet()
    toProcess.removeAll(previousSuccessfulTargets)
    toProcess.toList()
  }

  private val retryExecution: BatchJobChunkExecution by lazy {
    BatchJobChunkExecution().apply {
      batchJob = job
      chunkNumber = execution.chunkNumber
      status = BatchJobChunkExecutionStatus.PENDING
    }
  }

  val retries: Int by lazy {
    previousExecutions.size
  }

  @Suppress("UNCHECKED_CAST")
  private val previousExecutions: List<BatchJobChunkExecution> by lazy {
    entityManager.createQuery(
      """
      from BatchJobChunkExecution 
      where chunkNumber = :chunkNumber 
          and batchJob.id = :batchJobId
          and status = :status
      """.trimIndent()
    )
      .setParameter("chunkNumber", execution.chunkNumber)
      .setParameter("batchJobId", execution.batchJob.id)
      .setParameter("status", BatchJobChunkExecutionStatus.FAILED)
      .setHint(
        "javax.persistence.lock.timeout",
        LockOptions.NO_WAIT
      )
      .resultList as List<BatchJobChunkExecution>
  }
}
