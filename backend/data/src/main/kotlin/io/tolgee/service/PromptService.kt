package io.tolgee.service

import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.model.Prompt

interface PromptService {
  fun translateAndUpdateTranslation(
    projectId: Long,
    data: PromptRunDto,
  )

  fun findPromptOrDefaultDto(
    projectId: Long,
    promptId: Long? = null,
  ): PromptDto

  fun findPrompt(
    projectId: Long,
    promptId: Long,
  ): Prompt
}
