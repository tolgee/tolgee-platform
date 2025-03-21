package io.tolgee.dtos.response

import io.tolgee.component.machineTranslation.providers.tolgee.OpenaiApiService

data class PromptResponseDto(
  val prompt: String,
  val result: String,
  val usage: OpenaiApiService.Companion.OpenaiUsage?
)
