package io.tolgee.hateoas.llmProvider

import io.tolgee.api.v2.controllers.LLMProviderController
import io.tolgee.configuration.tolgee.machineTranslation.LLMProperties
import io.tolgee.model.LLMProvider
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LLMServerProviderModelAssembler : RepresentationModelAssemblerSupport<
  LLMProperties.LLMProvider,
  LLMServerProviderModel,
  >(
  LLMProviderController::class.java,
  LLMServerProviderModel::class.java,
) {
  override fun toModel(entity: LLMProperties.LLMProvider): LLMServerProviderModel {
    return LLMServerProviderModel(
      name = entity.name,
      type = entity.type,
      priority = entity.priority,
      apiUrl = entity.apiUrl,
      model = entity.model,
      deployment = entity.deployment,
      keepAlive = entity.keepAlive,
      format = entity.format,
    )
  }
}
