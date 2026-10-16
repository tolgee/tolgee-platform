package io.tolgee.dtos.request.prompt

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.BasicPromptOption
import io.tolgee.model.enums.PromptType

data class PromptRunDto(
  val template: String?,
  var keyId: Long,
  var targetLanguageId: Long,
  var provider: String,
  var basicPromptOptions: List<BasicPromptOption>?,
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Prompt type, TRANSLATE when omitted")
  var type: PromptType = PromptType.TRANSLATE,
  var reviewCriteria: String? = null,
)
