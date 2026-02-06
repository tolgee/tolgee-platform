package io.tolgee.ee.service.batch

import io.tolgee.batch.BatchApiResultHandler
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.dtos.PromptResult
import io.tolgee.ee.service.LlmProviderService
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditsService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * Phase 2 of batch API processing: reads parsed [OpenAiBatchResult] entries
 * from the [OpenAiBatchJobTracker] and applies them as translations.
 *
 * Handles missing keys/languages gracefully (skips and logs) so that
 * deletions between submission and completion do not cause failures.
 * Reports per-item progress via [ProgressManager] for UI updates.
 */
@Service
class BatchApiResultApplier(
  private val openAiBatchJobTrackerRepository: OpenAiBatchJobTrackerRepository,
  @Lazy
  private val translationService: TranslationService,
  @Lazy
  private val progressManager: ProgressManager,
  private val languageService: LanguageService,
  private val keyService: KeyService,
  private val mtCreditsService: MtCreditsService,
  @Lazy
  private val llmProviderService: LlmProviderService,
  private val projectService: ProjectService,
  private val metrics: io.tolgee.Metrics,
) : BatchApiResultHandler,
  Logging {
  override fun applyResults(
    job: BatchJobDto,
    chunkExecutionId: Long,
  ) {
    val tracker =
      openAiBatchJobTrackerRepository.findByChunkExecutionId(chunkExecutionId)
        ?: throw IllegalStateException(
          "No tracker found for chunk execution $chunkExecutionId",
        )

    logger.debug(
      "Applying results for tracker ${tracker.id}, " +
        "job ${job.id}, chunk execution $chunkExecutionId",
    )

    tracker.status = OpenAiBatchTrackerStatus.APPLYING
    openAiBatchJobTrackerRepository.save(tracker)

    val results =
      tracker.results ?: run {
        logger.warn("Tracker ${tracker.id} has no results to apply")
        tracker.status = OpenAiBatchTrackerStatus.COMPLETED
        openAiBatchJobTrackerRepository.save(tracker)
        return
      }

    val successCount = results.count { it.translatedText != null }
    val failedCount = results.count { it.error != null }
    metrics.batchApiTranslationsProcessedCounter("success").increment(successCount.toDouble())
    metrics.batchApiTranslationsProcessedCounter("failed").increment(failedCount.toDouble())

    val keyIds = results.mapNotNull { it.keyId }.distinct()
    val languageIds = results.mapNotNull { it.languageId }.distinct()
    val keys = keyService.find(keyIds).associateBy { it.id }
    val languages = languageService.findByIdIn(languageIds.toSet()).associateBy { it.id }

    for (result in results) {
      if (result.translatedText == null) {
        logger.debug(
          "Skipping result for key ${result.keyId}, language ${result.languageId}: " +
            "${result.error ?: "no translation"}",
        )
        continue
      }

      val key = keys[result.keyId]
      if (key == null) {
        logger.warn("Key ${result.keyId} not found, skipping")
        continue
      }

      val language = languages[result.languageId]
      if (language == null) {
        logger.warn("Language ${result.languageId} not found, skipping")
        continue
      }

      try {
        translationService.setTranslationText(key, language, result.translatedText)
        progressManager.reportSingleChunkProgress(job.id)
      } catch (e: Exception) {
        logger.error(
          "Failed to apply translation for key ${result.keyId}, " +
            "language ${result.languageId}: ${e.message}",
          e,
        )
      }
    }

    try {
      consumeCreditsForBatch(job, tracker.providerId, results)
    } catch (e: Exception) {
      logger.error(
        "Failed to consume credits for batch job ${job.id}: ${e.message}. " +
          "Translations were applied successfully but credits were not consumed.",
        e,
      )
    }

    tracker.status = OpenAiBatchTrackerStatus.COMPLETED
    openAiBatchJobTrackerRepository.save(tracker)

    logger.info(
      "Batch API results applied: tracker={}, job={}, total={}, success={}, failed={}",
      tracker.id,
      job.id,
      results.size,
      successCount,
      failedCount,
    )
  }

  private fun consumeCreditsForBatch(
    job: BatchJobDto,
    providerId: Long?,
    results: List<io.tolgee.model.batch.OpenAiBatchResult>,
  ) {
    val projectId = job.projectId ?: return
    val projectDto = projectService.getDto(projectId)
    val organizationId = projectDto.organizationOwnerId

    val successfulResults = results.filter { it.translatedText != null }
    if (successfulResults.isEmpty()) return

    val totalPromptTokens = successfulResults.sumOf { it.promptTokens }
    val totalCompletionTokens = successfulResults.sumOf { it.completionTokens }

    val providerConfig =
      if (providerId != null) {
        llmProviderService.getProviderById(organizationId, providerId)
      } else {
        null
      } ?: llmProviderService.findBatchEnabledProvider(organizationId)

    if (providerConfig == null) {
      logger.warn("No provider config found for batch credit consumption, job ${job.id}")
      return
    }

    val usage =
      PromptResult.Usage(
        inputTokens = totalPromptTokens,
        outputTokens = totalCompletionTokens,
      )

    val priceInCents = llmProviderService.calculatePrice(providerConfig, usage, isBatchApi = true)

    if (priceInCents > 0) {
      logger.debug(
        "Consuming $priceInCents credits for batch job ${job.id}: " +
          "${successfulResults.size} successful translations, " +
          "$totalPromptTokens prompt tokens, $totalCompletionTokens completion tokens",
      )
      mtCreditsService.consumeCredits(organizationId, priceInCents)
    }
  }
}
