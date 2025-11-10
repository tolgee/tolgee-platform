package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.dtos.LlmProviderDto
import io.tolgee.ee.api.v2.controllers.LlmProviderController
import io.tolgee.hateoas.llmProvider.LlmProviderModel
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LlmProviderModelAssembler :
  RepresentationModelAssemblerSupport<LlmProviderDto, LlmProviderModel>(
    LlmProviderController::class.java,
    LlmProviderModel::class.java,
  ) {
  override fun toModel(entity: LlmProviderDto): LlmProviderModel {
    return LlmProviderModel(
      id = entity.id,
      name = entity.name,
      type = entity.type,
      priority = entity.priority,
      apiKey = entity.apiKey,
      apiUrl = entity.apiUrl,
      model = entity.model,
      deployment = entity.deployment,
      format = entity.format,
      reasoningEffort = entity.reasoningEffort,
    )
  }
}
