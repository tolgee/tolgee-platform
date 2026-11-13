package io.tolgee.dtos.request.prompt

import io.tolgee.model.enums.BasicPromptOption
import io.tolgee.model.enums.PromptType

data class PromptRunDto(
  val template: String?,
  var keyId: Long,
  var targetLanguageId: Long,
  var provider: String,
  var basicPromptOptions: List<BasicPromptOption>?,
  val type: PromptType = PromptType.TRANSLATE,
)
