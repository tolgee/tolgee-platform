package io.tolgee.service

import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.model.Prompt
import io.tolgee.model.enums.LLMProviderPriority
import org.springframework.stereotype.Service

@Service
class PromptServiceOssImpl : PromptService {
  override fun translateAndUpdateTranslation(
    projectId: Long,
    data: PromptRunDto,
    priority: LLMProviderPriority?,
  ) {
    throw UnsupportedOperationException("Not included in OSS")
  }

  override fun findPromptOrDefaultDto(
    projectId: Long,
    promptId: Long?,
  ): PromptDto {
    throw UnsupportedOperationException("Not included in OSS")
  }

  override fun findPrompt(
    projectId: Long,
    promptId: Long,
  ): Prompt {
    throw UnsupportedOperationException("Not included in OSS")
  }
}
