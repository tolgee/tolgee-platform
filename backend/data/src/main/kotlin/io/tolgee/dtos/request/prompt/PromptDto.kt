package io.tolgee.dtos.request.prompt

data class PromptDto(
  val name: String,
  val template: String,
  val providerName: String,
)
