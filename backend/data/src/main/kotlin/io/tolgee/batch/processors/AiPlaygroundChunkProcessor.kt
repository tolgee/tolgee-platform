package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.FailedDontRequeueException
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.RequeueWithDelayException
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.exceptions.*
import io.tolgee.model.batch.params.MachineTranslationJobParams
import io.tolgee.model.enums.LLMProviderPriority
import io.tolgee.service.AiPlaygroundResultService
import io.tolgee.service.PromptService
import io.tolgee.service.key.KeyService
import io.tolgee.service.machineTranslation.MtServiceConfigService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class AiPlaygroundChunkProcessor(
  private val keyService: KeyService,
  private val promptService: PromptService,
  private val mtServiceConfigService: MtServiceConfigService,
  private val currentDateProvider: CurrentDateProvider,
  private val aiPlaygroundResultService: AiPlaygroundResultService,
) : ChunkProcessor<MachineTranslationRequest, MachineTranslationJobParams, BatchTranslationTargetItem> {
  override fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
  ) {
    val keys = keyService.find(chunk.map { it.keyId }).associateBy { it.id }

    iterateCatching(chunk, coroutineContext) { item ->
      val (keyId, languageId) = item
      val key = keys[keyId] ?: return@iterateCatching
      val llmPrompt = getParams(job).llmPrompt ?: throw Error("LlmPrompt required")

      val result =
        promptService.translate(
          job.projectId!!,
          PromptRunDto(
            template = llmPrompt.template,
            keyId = key.id,
            targetLanguageId = languageId,
            provider = llmPrompt.providerName,
          ),
          priority = LLMProviderPriority.LOW,
        )

      aiPlaygroundResultService.setResult(
        projectId = job.projectId!!,
        userId = job.authorId!!,
        keyId = key.id,
        languageId = languageId,
        translation = result.translated,
        contextDescription = result.contextDescription,
      )
    }
  }

  override fun getParamsType(): Class<MachineTranslationJobParams> {
    return MachineTranslationJobParams::class.java
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
    projectId ?: throw IllegalArgumentException("Project id is required")
    val languageIds = request.targetLanguageIds
    val services = mtServiceConfigService.getPrimaryServices(languageIds, projectId).values.toSet()
    if (services.map { it?.serviceType }.contains(MtServiceType.PROMPT)) {
      return 2
    }
    return 5
  }

  override fun getTargetItemType(): Class<BatchTranslationTargetItem> {
    return BatchTranslationTargetItem::class.java
  }

  override fun getParams(data: MachineTranslationRequest): MachineTranslationJobParams {
    return MachineTranslationJobParams().apply {
      this.targetLanguageIds = data.targetLanguageIds
      this.llmPrompt = data.llmPrompt
    }
  }

  fun iterateCatching(
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
    fn: (item: BatchTranslationTargetItem) -> Unit,
  ) {
    val successfulTargets = mutableListOf<BatchTranslationTargetItem>()
    chunk.forEach { item ->
      coroutineContext.ensureActive()
      try {
        fn(item)
        successfulTargets.add(item)
      } catch (e: OutOfCreditsException) {
        throw FailedDontRequeueException(Message.OUT_OF_CREDITS, successfulTargets, e)
      } catch (e: TranslationApiRateLimitException) {
        throw RequeueWithDelayException(
          Message.TRANSLATION_API_RATE_LIMIT,
          successfulTargets,
          e,
          (e.retryAt - currentDateProvider.date.time).toInt(),
          increaseFactor = 1,
          maxRetries = -1,
        )
      } catch (e: PlanTranslationLimitExceeded) {
        throw FailedDontRequeueException(Message.PLAN_TRANSLATION_LIMIT_EXCEEDED, successfulTargets, e)
      } catch (e: TranslationSpendingLimitExceeded) {
        throw FailedDontRequeueException(Message.TRANSLATION_SPENDING_LIMIT_EXCEEDED, successfulTargets, e)
      } catch (e: FormalityNotSupportedException) {
        throw FailedDontRequeueException(e.tolgeeMessage!!, successfulTargets, e)
      } catch (e: LanguageNotSupportedException) {
        throw FailedDontRequeueException(e.tolgeeMessage!!, successfulTargets, e)
      } catch (e: Throwable) {
        throw RequeueWithDelayException(Message.TRANSLATION_FAILED, successfulTargets, e)
      }
    }
  }
}
