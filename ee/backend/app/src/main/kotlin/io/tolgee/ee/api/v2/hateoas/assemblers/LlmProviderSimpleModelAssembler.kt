package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.ee.api.v2.controllers.LlmProviderController
import io.tolgee.hateoas.llmProvider.LlmProviderSimpleModel
import io.tolgee.model.LlmProvider
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LlmProviderSimpleModelAssembler :
  RepresentationModelAssemblerSupport<LlmProviderInterface, LlmProviderSimpleModel>(
    LlmProviderController::class.java,
    LlmProviderSimpleModel::class.java,
  ) {
  override fun toModel(entity: LlmProviderInterface): LlmProviderSimpleModel {
    return LlmProviderSimpleModel(
      entity.name,
      if (entity is LlmProvider) "organization" else "server",
      entity.type,
    )
  }
}
