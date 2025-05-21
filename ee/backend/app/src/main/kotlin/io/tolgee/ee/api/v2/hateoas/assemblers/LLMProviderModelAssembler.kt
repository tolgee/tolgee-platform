package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.dtos.LLMProviderDto
import io.tolgee.ee.api.v2.controllers.LLMProviderController
import io.tolgee.hateoas.llmProvider.LlmProviderModel
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LLMProviderModelAssembler : RepresentationModelAssemblerSupport<LLMProviderDto, LlmProviderModel>(
  LLMProviderController::class.java,
  LlmProviderModel::class.java,
) {
  override fun toModel(entity: LLMProviderDto): LlmProviderModel {
    return LlmProviderModel(
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
