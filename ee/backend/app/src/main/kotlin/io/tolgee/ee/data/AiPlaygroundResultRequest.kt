package io.tolgee.ee.data

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.PromptType
import jakarta.validation.constraints.NotEmpty

data class AiPlaygroundResultRequest(
  @field:NotEmpty
  var keys: List<Long>? = null,
  @field:NotEmpty
  var languages: List<Long>? = null,
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Result type, TRANSLATE when omitted")
  var type: PromptType = PromptType.TRANSLATE,
)
