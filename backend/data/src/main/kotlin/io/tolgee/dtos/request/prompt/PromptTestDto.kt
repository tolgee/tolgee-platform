package io.tolgee.dtos.request.prompt

data class PromptTestDto (
  val template: String,
  var projectId: Long,
  var keyId: Long,
  var targetLanguageId: Long,
)
