package io.tolgee.batch.request

import io.tolgee.dtos.request.prompt.PromptDto
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

class MachineTranslationRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Size(min = 1)
  var targetLanguageIds: List<Long> = listOf()

  var llmPrompt: PromptDto? = null
}
