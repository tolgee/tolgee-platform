package io.tolgee.dtos.response.prompt

data class PromptResponseUsageDto(
  val inputTokens: Long? = null,
  val outputTokens: Long? = null,
  val cachedTokens: Long? = null,
)
