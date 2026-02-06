package io.tolgee.ee.service.batch

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.Timer
import io.tolgee.batch.BatchApiPhase
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.ProgressManager
import io.tolgee.component.LockingProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.ee.service.OpenAiBatchApiService
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.OpenAiBatchJobTracker
import io.tolgee.model.batch.OpenAiBatchResult
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import io.tolgee.service.project.ProjectService
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.Duration

/**
 * Periodically polls the OpenAI Batch API for pending batch jobs.
 *
 * Registered via [SchedulingManager] on [ApplicationReadyEvent] with a configurable
 * poll interval ([BatchProperties.batchApiPollIntervalSeconds]). Uses
 * [LockingProvider] to ensure only one instance polls at a time in a
 * multi-replica deployment.
 *
 * When a batch completes, this service downloads the result JSONL, parses it
 * into [OpenAiBatchResult] objects, stores them on the [OpenAiBatchJobTracker],
 * and re-queues the chunk execution as PENDING for Phase 2 processing.
 *
 * Each tracker is polled in its own transaction so that a failure in one
 * tracker does not block others.
 */
@Component
class OpenAiBatchPoller(
  private val schedulingManager: SchedulingManager,
  private val lockingProvider: LockingProvider,
  @Lazy
  private val openAiBatchApiService: OpenAiBatchApiService,
  private val openAiBatchJobTrackerRepository: OpenAiBatchJobTrackerRepository,
  private val entityManager: EntityManager,
  private val transactionManager: PlatformTransactionManager,
  private val batchProperties: BatchProperties,
  private val objectMapper: ObjectMapper,
  @Lazy
  private val batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue,
  @Lazy
  private val projectService: ProjectService,
  @Lazy
  private val llmProviderService: io.tolgee.ee.service.LlmProviderService,
  private val metrics: io.tolgee.Metrics,
  @Lazy
  private val progressManager: ProgressManager,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun startPolling() {
    val period = Duration.ofSeconds(batchProperties.batchApiPollIntervalSeconds.toLong())
    schedulingManager.scheduleWithFixedDelay(::pollAllPending, period)
    logger.debug("Scheduled OpenAI batch poller with period: {}", period)
  }

  fun pollAllPending() {
    lockingProvider.withLockingIfFree(POLL_LOCK_NAME, Duration.ofMinutes(5)) {
      doPollAllPending()
    }
  }

  private fun doPollAllPending() {
    val sample = Timer.start()
    try {
      doPollAllPendingInner()
    } finally {
      sample.stop(metrics.batchApiPollDurationSeconds)
      lastPollTimestamp = System.currentTimeMillis()
    }
  }

  private fun doPollAllPendingInner() {
    val trackers =
      executeInNewTransaction(transactionManager) {
        openAiBatchJobTrackerRepository.findAndLockByStatusIn(
          listOf(OpenAiBatchTrackerStatus.SUBMITTED, OpenAiBatchTrackerStatus.IN_PROGRESS),
        )
      }

    if (trackers.isEmpty()) {
      return
    }

    logger.debug("Polling ${trackers.size} pending OpenAI batch trackers")

    for (tracker in trackers) {
      try {
        executeInNewTransaction(transactionManager) {
          pollSingleBatch(tracker.id)
        }
      } catch (e: Exception) {
        logger.error("Error polling OpenAI batch tracker ${tracker.id}: ${e.message}", e)
      }
    }
  }

  private fun pollSingleBatch(trackerId: Long) {
    val tracker =
      entityManager.find(OpenAiBatchJobTracker::class.java, trackerId)
        ?: run {
          logger.warn("Tracker $trackerId not found, skipping")
          return
        }

    val providerConfig = getProviderConfig(tracker)
    val apiKey =
      providerConfig.apiKey ?: run {
        logger.error("No API key for tracker ${tracker.id}, marking as failed")
        tracker.status = OpenAiBatchTrackerStatus.FAILED
        tracker.errorMessage = "No API key configured for provider"
        return
      }
    val apiUrl = providerConfig.apiUrl ?: "https://api.openai.com"

    val statusResult =
      openAiBatchApiService.pollBatchStatus(
        apiKey = apiKey,
        apiUrl = apiUrl,
        batchId = tracker.openAiBatchId,
      )

    tracker.openAiStatus = statusResult.status
    statusResult.requestCounts?.let { counts ->
      tracker.totalRequests = counts.total
      tracker.completedRequests = counts.completed
      tracker.failedRequests = counts.failed
    }

    // Check if the batch has exceeded the configured timeout
    if (isTimedOut(tracker)) {
      handleTimeout(tracker, apiKey, apiUrl)
      return
    }

    when (statusResult.status) {
      "completed" -> handleCompleted(tracker, apiKey, apiUrl, statusResult.outputFileId)
      "failed" -> handleFailed(tracker, statusResult.error?.message ?: "Batch failed at OpenAI")
      "expired" -> handleFailed(tracker, "Batch expired at OpenAI")
      "cancelled", "cancelling" -> handleCancelled(tracker)
      "in_progress", "finalizing" -> {
        tracker.status = OpenAiBatchTrackerStatus.IN_PROGRESS
        logger.debug("Tracker ${tracker.id}: batch ${tracker.openAiBatchId} still in progress")
        reportProgress(tracker, BatchApiPhase.WAITING_FOR_OPENAI)
      }
      else -> {
        logger.debug(
          "Tracker ${tracker.id}: batch ${tracker.openAiBatchId} has status ${statusResult.status}",
        )
      }
    }
  }

  private fun handleCompleted(
    tracker: OpenAiBatchJobTracker,
    apiKey: String,
    apiUrl: String,
    outputFileId: String?,
  ) {
    if (outputFileId == null) {
      handleFailed(tracker, "Completed batch has no output file ID")
      return
    }

    tracker.openAiOutputFileId = outputFileId

    logger.debug("Downloading results for tracker ${tracker.id}, output file: $outputFileId")

    val resultBytes =
      openAiBatchApiService.downloadResults(
        apiKey = apiKey,
        apiUrl = apiUrl,
        outputFileId = outputFileId,
      )

    val results = parseResults(resultBytes)
    tracker.results = results
    tracker.status = OpenAiBatchTrackerStatus.RESULTS_READY

    metrics.batchApiJobsCompletedCounter("completed").increment()
    metrics.decrementBatchApiActiveJobs()

    logger.info(
      "Batch API job completed: tracker={}, openAiBatch={}, resultCount={}",
      tracker.id,
      tracker.openAiBatchId,
      results.size,
    )

    // Clean up uploaded files at OpenAI to prevent orphans
    cleanupFiles(tracker, apiKey, apiUrl)

    // Transition chunk execution back to PENDING so it gets re-queued
    val chunkExecution = tracker.chunkExecution
    val executionEntity =
      entityManager.find(BatchJobChunkExecution::class.java, chunkExecution.id)
    if (executionEntity != null) {
      executionEntity.status = BatchJobChunkExecutionStatus.PENDING
      entityManager.flush()

      batchJobChunkExecutionQueue.addToQueue(
        executionEntity,
        executionEntity.batchJob.jobCharacter,
      )
      logger.debug(
        "Re-queued chunk execution ${executionEntity.id} for result application",
      )
    }
  }

  private fun handleFailed(
    tracker: OpenAiBatchJobTracker,
    errorMessage: String,
  ) {
    logger.error(
      "Batch API job failed: tracker={}, openAiBatch={}, error={}",
      tracker.id,
      tracker.openAiBatchId,
      errorMessage,
    )
    metrics.batchApiJobsCompletedCounter("failed").increment()
    metrics.decrementBatchApiActiveJobs()
    tracker.status = OpenAiBatchTrackerStatus.FAILED
    tracker.errorMessage = errorMessage

    val chunkExecution = tracker.chunkExecution
    val executionEntity =
      entityManager.find(BatchJobChunkExecution::class.java, chunkExecution.id)
    if (executionEntity != null) {
      executionEntity.status = BatchJobChunkExecutionStatus.FAILED
      executionEntity.errorKey = "openai_batch_failed"
    }
  }

  private fun handleCancelled(tracker: OpenAiBatchJobTracker) {
    logger.info(
      "Batch API job cancelled: tracker={}, openAiBatch={}",
      tracker.id,
      tracker.openAiBatchId,
    )
    metrics.batchApiJobsCompletedCounter("cancelled").increment()
    metrics.decrementBatchApiActiveJobs()
    tracker.status = OpenAiBatchTrackerStatus.CANCELLED

    val chunkExecution = tracker.chunkExecution
    val executionEntity =
      entityManager.find(BatchJobChunkExecution::class.java, chunkExecution.id)
    if (executionEntity != null) {
      executionEntity.status = BatchJobChunkExecutionStatus.CANCELLED
    }
  }

  private fun isTimedOut(tracker: OpenAiBatchJobTracker): Boolean {
    val createdAt = tracker.createdAt ?: return false
    val maxWaitMs = batchProperties.batchApiMaxWaitHours * 3600 * 1000L
    return System.currentTimeMillis() - createdAt.time > maxWaitMs
  }

  private fun handleTimeout(
    tracker: OpenAiBatchJobTracker,
    apiKey: String,
    apiUrl: String,
  ) {
    val elapsedHours =
      tracker.createdAt?.let {
        (System.currentTimeMillis() - it.time) / 3600000.0
      } ?: 0.0

    logger.warn(
      "Batch API job timed out: tracker={}, openAiBatch={}, elapsedHours={:.1f}, maxWaitHours={}",
      tracker.id,
      tracker.openAiBatchId,
      elapsedHours,
      batchProperties.batchApiMaxWaitHours,
    )

    try {
      openAiBatchApiService.cancelBatch(
        apiKey = apiKey,
        apiUrl = apiUrl,
        batchId = tracker.openAiBatchId,
      )
    } catch (e: Exception) {
      logger.warn("Failed to cancel timed-out batch ${tracker.openAiBatchId}: ${e.message}")
    }

    handleFailed(tracker, "Batch timed out after ${batchProperties.batchApiMaxWaitHours}h")
  }

  private fun cleanupFiles(
    tracker: OpenAiBatchJobTracker,
    apiKey: String,
    apiUrl: String,
  ) {
    try {
      openAiBatchApiService.deleteFile(apiKey, apiUrl, tracker.openAiInputFileId)
    } catch (e: Exception) {
      logger.warn("Failed to delete input file ${tracker.openAiInputFileId}: ${e.message}")
    }

    tracker.openAiOutputFileId?.let { outputFileId ->
      try {
        openAiBatchApiService.deleteFile(apiKey, apiUrl, outputFileId)
      } catch (e: Exception) {
        logger.warn("Failed to delete output file $outputFileId: ${e.message}")
      }
    }

    tracker.openAiErrorFileId?.let { errorFileId ->
      try {
        openAiBatchApiService.deleteFile(apiKey, apiUrl, errorFileId)
      } catch (e: Exception) {
        logger.warn("Failed to delete error file $errorFileId: ${e.message}")
      }
    }
  }

  fun parseResults(resultBytes: ByteArray): List<OpenAiBatchResult> {
    val lines = String(resultBytes, Charsets.UTF_8).lines().filter { it.isNotBlank() }
    return lines.mapNotNull { line ->
      try {
        val node = objectMapper.readTree(line)
        val customId = node.get("custom_id")?.asText() ?: return@mapNotNull null
        val parts = customId.split(":")
        if (parts.size < 3) return@mapNotNull null

        val keyId = parts[1].toLongOrNull() ?: return@mapNotNull null
        val languageId = parts[2].toLongOrNull() ?: return@mapNotNull null

        val error = node.get("error")
        if (error != null && !error.isNull) {
          return@mapNotNull OpenAiBatchResult(
            customId = customId,
            keyId = keyId,
            languageId = languageId,
            translatedText = null,
            contextDescription = null,
            error = error.get("message")?.asText() ?: "Unknown error",
          )
        }

        val response = node.get("response") ?: return@mapNotNull null
        val body = response.get("body") ?: return@mapNotNull null
        val choices = body.get("choices") ?: return@mapNotNull null
        val firstChoice = choices.get(0) ?: return@mapNotNull null
        val message = firstChoice.get("message") ?: return@mapNotNull null
        val content = message.get("content")?.let { if (it.isNull) null else it.asText() }

        val usage = body.get("usage")
        val promptTokens = usage?.get("prompt_tokens")?.asLong() ?: 0
        val completionTokens = usage?.get("completion_tokens")?.asLong() ?: 0

        OpenAiBatchResult(
          customId = customId,
          keyId = keyId,
          languageId = languageId,
          translatedText = content,
          contextDescription = null,
          promptTokens = promptTokens,
          completionTokens = completionTokens,
        )
      } catch (e: Exception) {
        logger.warn("Failed to parse batch result line: ${e.message}")
        null
      }
    }
  }

  private fun reportProgress(
    tracker: OpenAiBatchJobTracker,
    phase: BatchApiPhase,
  ) {
    val jobId = tracker.batchJob.id
    try {
      progressManager.reportExternalProgress(
        jobId = jobId,
        completedRequests = tracker.completedRequests,
        totalRequests = tracker.totalRequests,
        phase = phase,
      )
    } catch (e: Exception) {
      logger.debug("Failed to report progress for tracker ${tracker.id}: ${e.message}")
    }
  }

  private fun getProviderConfig(tracker: OpenAiBatchJobTracker): io.tolgee.dtos.LlmProviderDto {
    val projectId =
      tracker.batchJob.project?.id
        ?: throw IllegalStateException("Batch job has no project")
    val projectDto = projectService.getDto(projectId)
    val organizationId = projectDto.organizationOwnerId

    // Use the provider ID stored on the tracker (set during submission)
    val providerId = tracker.providerId
    if (providerId != null) {
      val provider = llmProviderService.getProviderById(organizationId, providerId)
      if (provider != null) return provider
      logger.warn("Provider $providerId not found, falling back to batch-enabled provider")
    }

    // Fallback: find any batch-enabled provider for this organization
    return llmProviderService.findBatchEnabledProvider(organizationId)
      ?: throw IllegalStateException("No batch-enabled provider found for organization $organizationId")
  }

  @Volatile
  var lastPollTimestamp: Long = 0L

  companion object {
    private const val POLL_LOCK_NAME = "openai_batch_poller_lock"
  }
}
