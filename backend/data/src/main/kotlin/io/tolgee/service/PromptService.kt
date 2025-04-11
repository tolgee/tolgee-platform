package io.tolgee.service

import com.fasterxml.jackson.databind.JsonNode
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.dtos.response.prompt.PromptResponseUsageDto
import io.tolgee.model.Prompt
import io.tolgee.model.enums.LLMProviderPriority

interface PromptService {
  fun translateAndUpdateTranslation(
    projectId: Long,
    data: PromptRunDto,
    priority: LLMProviderPriority?,
  )

  fun findPromptOrDefaultDto(
    projectId: Long,
    promptId: Long? = null,
  ): PromptDto

  fun findPrompt(
    projectId: Long,
    promptId: Long,
  ): Prompt

  companion object {
    class PromptResult(
      val response: String,
      val usage: PromptResponseUsageDto?,
      var parsedJson: JsonNode? = null,
      var price: Int = 0,
    )
  }
}
