package io.tolgee.dtos.response.prompt

data class PromptResponseDto(
  val prompt: String,
  val result: String,
  val usage: PromptResponseUsageDto?,
)
