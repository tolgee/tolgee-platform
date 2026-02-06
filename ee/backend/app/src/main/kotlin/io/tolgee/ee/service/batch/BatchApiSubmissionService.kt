package io.tolgee.ee.service.batch

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.batch.BatchApiSubmitter
import io.tolgee.batch.WaitingForExternalException
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.dtos.LlmParams
import io.tolgee.ee.service.LlmProviderService
import io.tolgee.ee.service.OpenAiBatchApiService
import io.tolgee.ee.service.prompt.PromptServiceEeImpl
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.OpenAiBatchJobTracker
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditsService
import io.tolgee.service.project.ProjectService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * Phase 1 of batch API processing: builds a JSONL request file, submits it to the
 * OpenAI Batch API, creates an [OpenAiBatchJobTracker], and throws
 * [WaitingForExternalException] to signal the framework.
 *
 * The JSONL `custom_id` field encodes `{jobId}:{keyId}:{languageId}` so that
 * [BatchApiResultApplier] can map results back to the correct translation keys.
 *
 * Prompt construction reuses Tolgee's standard prompt pipeline via
 * [PromptServiceEeImpl] to ensure all context (glossary, ICU info, key descriptions,
 * translation memory, etc.) is included.
 */
@Service
class BatchApiSubmissionService(
  @Lazy
  private val openAiBatchApiService: OpenAiBatchApiService,
  private val openAiBatchJobTrackerRepository: OpenAiBatchJobTrackerRepository,
  private val entityManager: EntityManager,
  private val objectMapper: ObjectMapper,
  @Lazy
  private val llmProviderService: LlmProviderService,
  private val projectService: ProjectService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val mtCreditsService: MtCreditsService,
  private val batchApiValidationService: BatchApiValidationService,
  private val metrics: io.tolgee.Metrics,
  @Lazy
  private val promptService: PromptServiceEeImpl,
) : BatchApiSubmitter,
  Logging {
  override fun submitBatch(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    chunkExecution: BatchJobChunkExecution,
  ) {
    val projectId = job.projectId ?: throw IllegalStateException("Project ID is required for batch API")
    val projectDto = projectService.getDto(projectId)
    val organizationId = projectDto.organizationOwnerId

    mtCreditsService.checkPositiveBalance(organizationId)

    val providerConfig =
      llmProviderService.findBatchEnabledProvider(organizationId)
        ?: throw IllegalStateException("No batch-enabled LLM provider configured for organization $organizationId")

    batchApiValidationService.validateBatchSubmission(
      itemCount = chunk.size,
      organizationId = organizationId,
      providerType = providerConfig.type,
      batchApiEnabled = providerConfig.batchApiEnabled,
    )

    val apiKey = providerConfig.apiKey ?: throw IllegalStateException("API key is required for batch API")
    val apiUrl = providerConfig.apiUrl ?: "https://api.openai.com"
    val model = providerConfig.model ?: "gpt-4o-mini"

    val promptDto = promptService.findPromptOrDefaultDto(projectId, null)

    val jsonlContent = buildJsonlContent(job, projectId, chunk, model, promptDto)

    logger.debug("Submitting batch API request for job ${job.id}, chunk execution ${chunkExecution.id}")

    val result =
      openAiBatchApiService.submitBatch(
        apiKey = apiKey,
        apiUrl = apiUrl,
        jsonlContent = jsonlContent,
        model = model,
        metadata = mapOf("tolgee_job_id" to job.id.toString()),
      )

    val tracker =
      OpenAiBatchJobTracker().apply {
        batchJob = chunkExecution.batchJob
        this.chunkExecution = chunkExecution
        openAiBatchId = result.batchId
        openAiInputFileId = result.inputFileId
        status = OpenAiBatchTrackerStatus.SUBMITTED
        totalRequests = chunk.size
        providerId = providerConfig.id
      }
    openAiBatchJobTrackerRepository.save(tracker)
    entityManager.flush()

    metrics.batchApiJobsSubmittedCounter.increment()
    metrics.incrementBatchApiActiveJobs()

    logger.info(
      "Batch API job submitted: job={}, chunkExecution={}, tracker={}, openAiBatch={}, items={}, provider={}",
      job.id,
      chunkExecution.id,
      tracker.id,
      result.batchId,
      chunk.size,
      providerConfig.name,
    )

    throw WaitingForExternalException("Batch submitted to OpenAI, batch ID: ${result.batchId}")
  }

  private fun buildJsonlContent(
    job: BatchJobDto,
    projectId: Long,
    chunk: List<BatchTranslationTargetItem>,
    model: String,
    promptDto: io.tolgee.dtos.request.prompt.PromptDto,
  ): ByteArray {
    val template = promptDto.template ?: promptService.getDefaultPrompt().template!!

    val lines =
      chunk.mapNotNull { item ->
        try {
          val prompt =
            promptService.getPrompt(
              projectId = projectId,
              template = template,
              keyId = item.keyId,
              targetLanguageId = item.languageId,
              options = promptDto.basicPromptOptions,
            )

          val llmParams = promptService.getLlmParamsFromPrompt(prompt, item.keyId, LlmProviderPriority.LOW)
          val content = buildMessageContent(llmParams)

          val customId = "${job.id}:${item.keyId}:${item.languageId}"

          val requestBody =
            mapOf(
              "custom_id" to customId,
              "method" to "POST",
              "url" to "/v1/chat/completions",
              "body" to
                buildRequestBody(model, content, llmParams.shouldOutputJson),
            )

          objectMapper.writeValueAsString(requestBody)
        } catch (e: Exception) {
          logger.warn(
            "Failed to build prompt for key={}, language={}: {}",
            item.keyId,
            item.languageId,
            e.message,
          )
          null
        }
      }

    return lines.joinToString("\n").toByteArray(Charsets.UTF_8)
  }

  private fun buildMessageContent(llmParams: LlmParams): List<Map<String, Any?>> {
    val content = mutableListOf<Map<String, Any?>>()

    for (msg in llmParams.messages) {
      when (msg.type) {
        LlmParams.Companion.LlmMessageType.TEXT -> {
          if (msg.text != null) {
            content.add(mapOf("type" to "text", "text" to msg.text))
          }
        }
        LlmParams.Companion.LlmMessageType.IMAGE -> {
          // Images are skipped in batch API requests as they significantly
          // increase payload size and the Batch API has file size limits.
          logger.debug("Skipping image content in batch API JSONL request")
        }
      }
    }

    if (llmParams.shouldOutputJson) {
      content.add(mapOf("type" to "text", "text" to "Strictly return only valid json!"))
    }

    return content
  }

  private fun buildRequestBody(
    model: String,
    content: List<Map<String, Any?>>,
    shouldOutputJson: Boolean,
  ): Map<String, Any?> {
    val body =
      mutableMapOf<String, Any?>(
        "model" to model,
        "messages" to
          listOf(
            mapOf("role" to "user", "content" to content),
          ),
      )

    if (shouldOutputJson) {
      body["response_format"] =
        mapOf(
          "type" to "json_schema",
          "json_schema" to
            mapOf(
              "name" to "simple_response",
              "schema" to
                mapOf(
                  "type" to "object",
                  "properties" to
                    mapOf(
                      "output" to mapOf("type" to "string"),
                      "contextDescription" to mapOf("type" to "string"),
                    ),
                  "required" to listOf("output", "contextDescription"),
                  "additionalProperties" to false,
                ),
              "strict" to true,
            ),
        )
    }

    return body
  }
}
