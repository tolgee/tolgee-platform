package io.tolgee.service

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.dtos.aiReview.AiReviewRunResult
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.exceptions.NotImplementedInOss
import io.tolgee.model.Prompt
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.PromptType
import org.springframework.stereotype.Service

@Service
class PromptServiceOssImpl : PromptService {
  override fun translate(
    projectId: Long,
    data: PromptRunDto,
    priority: LlmProviderPriority?,
  ): MtValueProvider.MtResult {
    throw NotImplementedInOss()
  }

  override fun review(
    projectId: Long,
    data: PromptRunDto,
    priority: LlmProviderPriority?,
  ): AiReviewRunResult {
    throw NotImplementedInOss()
  }

  override fun findPromptOrDefaultDto(
    projectId: Long,
    promptId: Long?,
    type: PromptType,
  ): PromptDto {
    throw NotImplementedInOss()
  }

  override fun findPrompt(
    projectId: Long,
    promptId: Long,
  ): Prompt {
    throw NotImplementedInOss()
  }

  override fun deleteAllByProjectId(projectId: Long) {
    throw NotImplementedInOss()
  }
}
