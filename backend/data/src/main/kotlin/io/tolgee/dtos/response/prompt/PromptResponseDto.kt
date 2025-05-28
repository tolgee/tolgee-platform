package io.tolgee.dtos.response.prompt

import com.fasterxml.jackson.databind.JsonNode

data class PromptResponseDto(
  val prompt: String,
  val result: String,
  val parsedJson: JsonNode?,
  val price: Int?,
  val usage: PromptResponseUsageDto?,
)
