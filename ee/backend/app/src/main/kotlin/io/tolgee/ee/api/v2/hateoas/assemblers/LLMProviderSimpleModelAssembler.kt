package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.ee.api.v2.controllers.LLMProviderController
import io.tolgee.hateoas.llmProvider.LlmProviderSimpleModel
import io.tolgee.model.LLMProvider
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LLMProviderSimpleModelAssembler :
  RepresentationModelAssemblerSupport<LLMProviderInterface, LlmProviderSimpleModel>(
    LLMProviderController::class.java,
    LlmProviderSimpleModel::class.java,
  ) {
  override fun toModel(entity: LLMProviderInterface): LlmProviderSimpleModel {
    return LlmProviderSimpleModel(
      entity.name,
      if (entity is LLMProvider) "organization" else "server",
      entity.type,
    )
  }
}
