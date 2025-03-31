package io.tolgee.dtos.request.prompt

data class PromptRunDto(
  val template: String,
  var keyId: Long,
  var targetLanguageId: Long,
  var provider: String,
)
