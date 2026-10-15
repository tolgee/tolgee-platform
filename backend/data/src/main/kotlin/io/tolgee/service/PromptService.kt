package io.tolgee.service

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.dtos.aiReview.AiReviewRunResult
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.model.Prompt
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.PromptType

interface PromptService {
  fun translate(
    projectId: Long,
    data: PromptRunDto,
    priority: LlmProviderPriority?,
  ): MtValueProvider.MtResult

  fun review(
    projectId: Long,
    data: PromptRunDto,
    priority: LlmProviderPriority?,
  ): AiReviewRunResult

  fun findPromptOrDefaultDto(
    projectId: Long,
    promptId: Long? = null,
    type: PromptType = PromptType.TRANSLATE,
  ): PromptDto

  fun findPrompt(
    projectId: Long,
    promptId: Long,
  ): Prompt

  fun deleteAllByProjectId(projectId: Long)
}
