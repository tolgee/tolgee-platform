package io.tolgee.ee.service.batch

import io.tolgee.batch.BatchApiCancellationHandler
import io.tolgee.ee.service.OpenAiBatchApiService
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import io.tolgee.service.project.ProjectService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class OpenAiBatchCancellationHandler(
  @Lazy
  private val openAiBatchApiService: OpenAiBatchApiService,
  private val openAiBatchJobTrackerRepository: OpenAiBatchJobTrackerRepository,
  @Lazy
  private val projectService: ProjectService,
  @Lazy
  private val llmProviderService: io.tolgee.ee.service.LlmProviderService,
) : BatchApiCancellationHandler,
  Logging {
  override fun cancelExternalBatch(chunkExecution: BatchJobChunkExecution) {
    val tracker = openAiBatchJobTrackerRepository.findByChunkExecutionId(chunkExecution.id)
    if (tracker == null) {
      logger.warn("No tracker found for chunk execution ${chunkExecution.id}, skipping external cancellation")
      return
    }

    if (tracker.status.isTerminal()) {
      logger.debug("Tracker ${tracker.id} is already in terminal status ${tracker.status}, skipping cancellation")
      return
    }

    val projectId =
      tracker.batchJob.project?.id
        ?: run {
          logger.error("Batch job has no project for tracker ${tracker.id}")
          tracker.status = OpenAiBatchTrackerStatus.CANCELLED
          return
        }

    try {
      val projectDto = projectService.getDto(projectId)
      val organizationId = projectDto.organizationOwnerId

      val providerConfig =
        tracker.providerId?.let { llmProviderService.getProviderById(organizationId, it) }
          ?: llmProviderService.findBatchEnabledProvider(organizationId)
          ?: run {
            logger.error("No batch-enabled provider found for organization $organizationId")
            tracker.status = OpenAiBatchTrackerStatus.CANCELLED
            return
          }
      val apiKey = providerConfig.apiKey
      val apiUrl = providerConfig.apiUrl ?: "https://api.openai.com"

      if (apiKey != null) {
        logger.debug("Cancelling OpenAI batch ${tracker.openAiBatchId} for tracker ${tracker.id}")
        openAiBatchApiService.cancelBatch(
          apiKey = apiKey,
          apiUrl = apiUrl,
          batchId = tracker.openAiBatchId,
        )
      } else {
        logger.warn("No API key available for cancelling batch ${tracker.openAiBatchId}")
      }
    } catch (e: Exception) {
      logger.warn(
        "Failed to cancel OpenAI batch ${tracker.openAiBatchId}: ${e.message}. " +
          "The poller will handle cleanup on next poll.",
        e,
      )
    }

    tracker.status = OpenAiBatchTrackerStatus.CANCELLED
  }

  private fun OpenAiBatchTrackerStatus.isTerminal(): Boolean =
    this == OpenAiBatchTrackerStatus.COMPLETED ||
      this == OpenAiBatchTrackerStatus.FAILED ||
      this == OpenAiBatchTrackerStatus.CANCELLED
}
