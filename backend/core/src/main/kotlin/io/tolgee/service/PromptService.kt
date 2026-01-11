package io.tolgee.service

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.model.Prompt
import io.tolgee.model.enums.LlmProviderPriority

interface PromptService {
  fun translate(
    projectId: Long,
    data: PromptRunDto,
    priority: LlmProviderPriority?,
  ): MtValueProvider.MtResult

  fun findPromptOrDefaultDto(
    projectId: Long,
    promptId: Long? = null,
  ): PromptDto

  fun findPrompt(
    projectId: Long,
    promptId: Long,
  ): Prompt

  fun deleteAllByProjectId(projectId: Long)
}
