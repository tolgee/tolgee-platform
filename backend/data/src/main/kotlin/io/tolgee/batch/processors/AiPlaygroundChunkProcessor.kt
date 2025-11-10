package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.MtProviderCatching
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.exceptions.InvalidStateException
import io.tolgee.model.batch.params.AiPlaygroundJobParams
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.key.Key
import io.tolgee.service.AiPlaygroundResultService
import io.tolgee.service.PromptService
import io.tolgee.service.key.KeyService
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class AiPlaygroundChunkProcessor(
  private val keyService: KeyService,
  private val promptService: PromptService,
  private val aiPlaygroundResultService: AiPlaygroundResultService,
  private val mtProviderCatching: MtProviderCatching,
) : ChunkProcessor<MachineTranslationRequest, AiPlaygroundJobParams, BatchTranslationTargetItem> {
  override fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
  ) {
    val keys = keyService.find(chunk.map { it.keyId }).associateBy { it.id }

    mtProviderCatching.iterateCatching(chunk, coroutineContext) { item ->
      val (keyId, languageId) = item
      val key = keys[keyId] ?: return@iterateCatching
      val llmPrompt = getParams(job).llmPrompt ?: throw InvalidStateException()
      translateAndSetResult(job, llmPrompt, key, languageId)
    }
  }

  fun translateAndSetResult(
    job: BatchJobDto,
    llmPrompt: PromptDto,
    key: Key,
    languageId: Long,
  ) {
    val result =
      promptService.translate(
        job.projectId!!,
        PromptRunDto(
          template = llmPrompt.template,
          keyId = key.id,
          targetLanguageId = languageId,
          provider = llmPrompt.providerName,
          basicPromptOptions = llmPrompt.basicPromptOptions,
        ),
        priority = LlmProviderPriority.LOW,
      )

    aiPlaygroundResultService.setResult(
      projectId = job.projectId,
      userId = job.authorId!!,
      keyId = key.id,
      languageId = languageId,
      translation = result.translated,
      contextDescription = result.contextDescription,
    )
  }

  override fun getParamsType(): Class<AiPlaygroundJobParams> {
    return AiPlaygroundJobParams::class.java
  }

  override fun getTarget(data: MachineTranslationRequest): List<BatchTranslationTargetItem> {
    return data.keyIds.flatMap { keyId ->
      data.targetLanguageIds.map { languageId ->
        BatchTranslationTargetItem(keyId, languageId)
      }
    }
  }

  override fun getMaxPerJobConcurrency(): Int {
    return 1
  }

  override fun getJobCharacter(): JobCharacter {
    return JobCharacter.SLOW
  }

  override fun getChunkSize(
    request: MachineTranslationRequest,
    projectId: Long?,
  ): Int {
    return 1
  }

  override fun getTargetItemType(): Class<BatchTranslationTargetItem> {
    return BatchTranslationTargetItem::class.java
  }

  override fun getParams(data: MachineTranslationRequest): AiPlaygroundJobParams {
    return AiPlaygroundJobParams().apply {
      this.targetLanguageIds = data.targetLanguageIds
      this.llmPrompt = data.llmPrompt
    }
  }
}
