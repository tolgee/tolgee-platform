package io.tolgee.ee.api.v2.hateoas.model.prompt

import org.springframework.hateoas.RepresentationModel

data class PromptResponseUsageModel(
  val inputTokens: Long? = null,
  val outputTokens: Long? = null,
  val cachedTokens: Long? = null,
) : RepresentationModel<PromptResponseUsageModel>()
