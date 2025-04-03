package io.tolgee.service

import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.model.Prompt

interface PromptService {
  fun translateAndUpdateTranslation(
    projectId: Long,
    data: PromptRunDto,
  )

  fun findPrompt(
    projectId: Long,
    promtId: Long,
  ): Prompt
}
