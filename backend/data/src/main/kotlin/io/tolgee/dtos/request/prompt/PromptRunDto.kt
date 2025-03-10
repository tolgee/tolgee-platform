package io.tolgee.dtos.request.prompt

import io.tolgee.model.enums.BasicPromptOption

data class PromptRunDto(
  val template: String?,
  var keyId: Long,
  var targetLanguageId: Long,
  var provider: String,
  var options: List<BasicPromptOption>?,
)
