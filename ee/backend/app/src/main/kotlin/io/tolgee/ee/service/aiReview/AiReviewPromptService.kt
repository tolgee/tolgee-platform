package io.tolgee.ee.service.aiReview

import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.ee.service.aiReview.scoring.AiReviewScoring
import io.tolgee.ee.service.aiReview.scoring.ReviewResultParser
import io.tolgee.ee.service.prompt.DefaultReviewPromptHelper
import io.tolgee.ee.service.prompt.PromptServiceEeImpl
import io.tolgee.model.Prompt
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.translation.Translation
import io.tolgee.service.translation.TranslationService
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class AiReviewPromptService(
  private val promptService: PromptServiceEeImpl,
  private val translationService: TranslationService,
  private val objectMapper: ObjectMapper,
) {
  fun runPlaygroundReview(
    organizationId: Long,
    projectId: Long,
    data: PromptRunDto,
  ): ReviewRunResult {
    val translation = translationService.getOrCreate(projectId, data.keyId, data.targetLanguageId)
    val prompt =
      DefaultReviewPromptHelper.get().copy(
        template = data.template,
        providerName = data.provider,
        basicPromptOptions = data.basicPromptOptions,
        reviewCriteria = data.reviewCriteria,
      )
    return reviewWithPrompt(
      organizationId,
      projectId,
      translation,
      prompt,
      LlmProviderPriority.HIGH,
      savedPrompt = null,
    )
  }

  fun runReview(
    organizationId: Long,
    projectId: Long,
    translation: Translation,
    promptId: Long?,
    priority: LlmProviderPriority,
  ): ReviewRunResult {
    val prompt = promptId?.let { promptService.findPrompt(projectId, it) }
    val promptDto = prompt?.toDto() ?: DefaultReviewPromptHelper.get()
    return reviewWithPrompt(organizationId, projectId, translation, promptDto, priority, savedPrompt = prompt)
  }

  fun reviewWithPrompt(
    organizationId: Long,
    projectId: Long,
    translation: Translation,
    prompt: PromptDto,
    priority: LlmProviderPriority,
    savedPrompt: Prompt?,
  ): ReviewRunResult {
    val rendered = renderReviewPrompt(projectId, translation, prompt)
    val params = promptService.getLlmParamsFromPrompt(rendered, translation.key.id, priority)
    val result =
      promptService.callProviderAndChargeCredits(
        organizationId,
        params.withSchema(),
        prompt.providerName,
      )
    val issues = ReviewResultParser.parse(result.response, objectMapper)
    return ReviewRunResult(
      issues = issues,
      score = AiReviewScoring.score(issues),
      rawResponse = result.response,
      providerName = result.providerName,
      model = result.model,
      promptId = savedPrompt?.id,
      promptName = prompt.name,
      promptRevision = savedPrompt?.revision,
      renderedPrompt = rendered,
      price = result.price,
      usage = result.usage,
    )
  }

  fun renderReviewPrompt(
    projectId: Long,
    translation: Translation,
    prompt: PromptDto,
  ): String =
    promptService.getPrompt(
      projectId,
      prompt.template ?: DefaultReviewPromptHelper.get().template!!,
      translation.key.id,
      translation.language.id,
      prompt.basicPromptOptions,
      prompt.reviewCriteria,
    )

  private fun Prompt.toDto(): PromptDto =
    PromptDto(
      name = name,
      providerName = providerName,
      template = template,
      basicPromptOptions = basicPromptOptions?.toList(),
      type = type,
      reviewCriteria = reviewCriteria,
    )

  private fun LlmParams.withSchema(): LlmParams =
    LlmParams(messages, shouldOutputJson, priority, AiReviewResponseSchema.SCHEMA)
}
