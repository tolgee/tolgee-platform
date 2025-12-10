package io.tolgee.ee.api.v2.hateoas.model.prompt

import io.tolgee.dtos.PromptResult
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

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
