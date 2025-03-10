package io.tolgee.model.batch.params

import io.tolgee.dtos.request.prompt.PromptDto

class MachineTranslationJobParams {
  var targetLanguageIds: List<Long> = mutableListOf()
  var llmPrompt: PromptDto? = null
}
