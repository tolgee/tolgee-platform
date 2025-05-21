package io.tolgee.dtos.request.prompt

import io.tolgee.model.enums.BasicPromptOption

data class PromptDto(
  val name: String,
  val template: String? = null,
  val providerName: String,
  var options: List<BasicPromptOption>? = null,
)
