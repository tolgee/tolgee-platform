package io.tolgee.dtos.request.prompt

import io.tolgee.model.enums.BasicPromptOption

data class PromptDto(
  val name: String,
  val providerName: String,
  val template: String? = null,
  var basicPromptOptions: List<BasicPromptOption>? = null,
)
