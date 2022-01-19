package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema

data class SuggestRequestDto(
  @Schema(description = "Key Id to get results for. Use when key is stored already.")
  var keyId: Long = 0,
  var targetLanguageId: Long = 0,
  @Schema(description = "Text value of base translation. Useful, when base translation is not stored yet.")
  var baseText: String? = null,
)
