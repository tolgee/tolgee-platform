package io.tolgee.ee.api.v2.hateoas.model.prompt

import io.tolgee.dtos.PromptResult
import io.tolgee.ee.service.prompt.PromptResultParser
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component
import kotlin.Long

@Component
class PromptResponseUsageModelAssembler : RepresentationModelAssembler<PromptResult.Usage, PromptResponseUsageModel> {
  override fun toModel(result: PromptResult.Usage): PromptResponseUsageModel {
    return PromptResponseUsageModel(
      inputTokens = result.inputTokens,
      outputTokens = result.outputTokens,
      cachedTokens = result.cachedTokens,
    )
  }
}
