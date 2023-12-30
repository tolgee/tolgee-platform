package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.MtServiceType

data class SuggestRequestDto(
  @Schema(description = "Key Id to get results for. Use when key is stored already.")
  var keyId: Long = 0,
  var targetLanguageId: Long = 0,
  @Schema(description = "Text value of base translation. Useful, when base translation is not stored yet.")
  var baseText: String? = null,
  @Schema(description = "List of services to use. If null, then all enabled services are used.")
  var services: Set<MtServiceType>? = null,
)
