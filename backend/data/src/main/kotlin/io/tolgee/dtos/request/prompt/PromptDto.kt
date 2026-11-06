package io.tolgee.dtos.request.prompt

import io.tolgee.model.enums.BasicPromptOption
import io.tolgee.model.enums.PromptType

data class PromptDto(
  val name: String,
  val providerName: String,
  val template: String? = null,
  var basicPromptOptions: List<BasicPromptOption>? = null,
  val type: PromptType = PromptType.TRANSLATE,
  val reviewCriteria: String? = null,
)
