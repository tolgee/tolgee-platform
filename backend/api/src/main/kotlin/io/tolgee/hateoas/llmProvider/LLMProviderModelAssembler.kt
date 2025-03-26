package io.tolgee.hateoas.llmProvider

import io.tolgee.api.v2.controllers.LLMProviderController
import io.tolgee.model.LLMProvider
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LLMProviderModelAssembler : RepresentationModelAssemblerSupport<LLMProvider, LLMProviderModel>(
  LLMProviderController::class.java,
  LLMProviderModel::class.java,
) {
  override fun toModel(entity: LLMProvider): LLMProviderModel {
    return LLMProviderModel(
      id = entity.id,
      name = entity.name,
      type = entity.type,
      priority = entity.priority,
      apiKey = entity.apiKey,
      apiUrl = entity.apiUrl,
      model = entity.model,
      deployment = entity.deployment,
      keepAlive = entity.keepAlive,
      format = entity.format,
    )
  }
}
