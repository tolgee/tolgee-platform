package io.tolgee.dtos.response.prompt

data class PromptResponseUsageDto(
  val totalTokens: Long? = null,
  val cachedTokens: Long? = null,
)
