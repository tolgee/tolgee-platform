package io.tolgee.dtos.request.prompt

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.BasicPromptOption
import io.tolgee.model.enums.PromptType

data class PromptDto(
  val name: String,
  val providerName: String,
  val template: String? = null,
  var basicPromptOptions: List<BasicPromptOption>? = null,
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Prompt type, TRANSLATE when omitted")
  val type: PromptType = PromptType.TRANSLATE,
  val reviewCriteria: String? = null,
) {
  fun toReviewRunDto(
    keyId: Long,
    targetLanguageId: Long,
  ) = PromptRunDto(
    template = template,
    keyId = keyId,
    targetLanguageId = targetLanguageId,
    provider = providerName,
    basicPromptOptions = basicPromptOptions,
    type = PromptType.REVIEW,
    reviewCriteria = reviewCriteria,
  )
}
