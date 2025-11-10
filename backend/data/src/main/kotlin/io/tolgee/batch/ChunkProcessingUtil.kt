package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.sentry.Sentry
import io.tolgee.activity.ActivityHolder
import io.tolgee.component.CurrentDateProvider
import io.tolgee.exceptions.ExceptionWithMessage
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.service.project.ProjectService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.apache.commons.lang3.exception.ExceptionUtils
import org.hibernate.LockOptions
import org.springframework.context.ApplicationContext
import java.util.Date
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow
import kotlin.system.measureTimeMillis

open class ChunkProcessingUtil(
  val execution: BatchJobChunkExecution,
  private val applicationContext: ApplicationContext,
  private val coroutineContext: CoroutineContext,
) : Logging {
  open fun processChunk() {
    val time =
      measureTimeMillis {
        try {
          handleActivity()
          processor.process(job, toProcess, coroutineContext) {
            if (it != toProcess.size) {
              progressManager.publishSingleChunkProgress(job.id, it)
            }
          }
          successfulTargets = toProcess
          execution.status = BatchJobChunkExecutionStatus.SUCCESS
        } catch (e: Throwable) {
          handleException(e)
        } finally {
          successfulTargets?.let {
            execution.successTargets = it
          }
        }
      }
    logger.debug("Chunk ${execution.id} executed in ${time}ms")
  }

  private fun handleActivity() {
    val activityRevision = activityHolder.activityRevision
    activityRevision.batchJobChunkExecution = execution
    val batchJobDto = batchJobService.getJobDto(job.id)
    batchJobDto.projectId?.let {
      val project = projectService.getDto(it)
      activityRevision.projectId = project.id
      activityRevision.organizationId = project.organizationOwnerId
    }
    activityHolder.activity = batchJobDto.type.activityType
    activityRevision.authorId = batchJobDto.authorId
  }

  private fun handleException(exception: Throwable) {
    if (exception is kotlinx.coroutines.CancellationException) {
      execution.status = BatchJobChunkExecutionStatus.CANCELLED
      return
    }

    execution.stackTrace = exception.stackTraceToString()
    execution.status = BatchJobChunkExecutionStatus.FAILED
    execution.errorMessage = (exception as? ExceptionWithMessage)?.tolgeeMessage
    execution.errorKey = ExceptionUtils.getRootCause(exception)?.javaClass?.simpleName

    logException(exception)

    if (exception is ChunkFailedException) {
      successfulTargets = exception.successfulTargets
      successfulTargets?.let { execution.successTargets = it }
    }

    if (exception is FailedDontRequeueException) {
      return
    }

    retryFailedExecution(exception)
  }

  private fun logException(exception: Throwable) {
    val knownCauses =
      listOf(
        OutOfCreditsException::class.java,
        LlmRateLimitedException::class.java,
      )

    val isKnownCause = knownCauses.any { ExceptionUtils.indexOfType(exception, it) > -1 }
    if (!isKnownCause) {
      Sentry.captureException(exception)
      logger.error(exception.message, exception)
    }
  }

  private fun retryFailedExecution(exception: Throwable) {
    var maxRetries = job.type.maxRetries
    var waitTime = job.type.defaultRetryWaitTimeInMs

    if (exception is RequeueWithDelayException) {
      maxRetries = exception.maxRetries
      waitTime = getWaitTime(exception)
    }

    logger.debug(
      "Total retries ${retries.values.sum()}, " +
        "retries for error key: $errorKeyRetries, max retries $maxRetries",
    )
    if (errorKeyRetries >= maxRetries && maxRetries != -1) {
      logger.debug("Max retries reached for job execution ${execution.id}")
      Sentry.captureException(exception)
      return
    }

    logger.debug("Retrying job execution ${execution.id} in ${waitTime}ms")
    retryExecution.executeAfter = Date(waitTime + currentDateProvider.date.time)
    execution.retry = true
  }

  private fun getWaitTime(exception: RequeueWithDelayException) =
    exception.delayInMs * (exception.increaseFactor.toDouble().pow(errorKeyRetries.toDouble())).toInt()

  private val job by lazy { batchJobService.getJobDto(execution.batchJob.id) }

  private val activityHolder by lazy {
    applicationContext.getBean(ActivityHolder::class.java)
  }

  private val entityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }

  private val currentDateProvider by lazy {
    applicationContext.getBean(CurrentDateProvider::class.java)
  }

  private val batchJobService by lazy {
    applicationContext.getBean(BatchJobService::class.java)
  }

  private val progressManager by lazy {
    applicationContext.getBean(ProgressManager::class.java)
  }

  private val processor by lazy {
    batchJobService.getProcessor(job.type)
  }

  private val projectService by lazy {
    applicationContext.getBean(ProjectService::class.java)
  }

  private var successfulTargets: List<Any>? = null

  private val toProcess by lazy {
    val toProcess = chunk.toMutableSet()
    toProcess.removeAll(previousSuccessfulTargets)
    toProcess.toList()
  }

  private val previousSuccessfulTargets by lazy {
    previousExecutions
      .flatMap {
        // this is important!!
        // we want the equals check to be run on the correct type with correct class instances
        convertChunkToItsType(it.successTargets)
      }.toSet()
  }

  /**
   * We need to convert the chunk to the right type, so we pass it to the processor correctly
   *
   * e.g. It can happen that the chunk is converted to a list of integers for caching, but
   * we actually need a list of Long
   */
  private val chunk by lazy {
    val chunked = job.chunkedTarget
    val chunk = chunked[execution.chunkNumber]
    convertChunkToItsType(chunk)
  }

  private fun convertChunkToItsType(chunk: List<Any>): List<Any> {
    val type =
      jacksonObjectMapper().typeFactory.constructCollectionType(List::class.java, processor.getTargetItemType())
    return jacksonObjectMapper().convertValue(chunk, type) as List<Any>
  }

  val retryExecution: BatchJobChunkExecution by lazy {
    BatchJobChunkExecution().apply {
      batchJob = entityManager.getReference(BatchJob::class.java, job.id)
      chunkNumber = execution.chunkNumber
      status = BatchJobChunkExecutionStatus.PENDING
    }
  }

  private val errorKeyRetries by lazy {
    val errorKey = execution.errorKey ?: throw IllegalStateException("Error key is not set")
    retries[errorKey] ?: 0
  }

  private val retries: Map<String?, Long> by lazy {
    previousExecutions.groupBy { it.errorKey }.map { it.key to it.value.size.toLong() }.toMap()
  }

  @Suppress("UNCHECKED_CAST")
  private val previousExecutions: List<BatchJobChunkExecution> by lazy {
    entityManager
      .createQuery(
        """
        from BatchJobChunkExecution 
        where chunkNumber = :chunkNumber 
            and batchJob.id = :batchJobId
            and status = :status
        """.trimIndent(),
      ).setParameter("chunkNumber", execution.chunkNumber)
      .setParameter("batchJobId", job.id)
      .setParameter("status", BatchJobChunkExecutionStatus.FAILED)
      .setHint(
        "jakarta.persistence.lock.timeout",
        LockOptions.NO_WAIT,
      ).resultList as List<BatchJobChunkExecution>
  }
}
