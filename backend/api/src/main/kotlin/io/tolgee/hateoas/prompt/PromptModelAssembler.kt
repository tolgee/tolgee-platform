package io.tolgee.hateoas.prompt

import io.tolgee.api.v2.controllers.PromptController
import io.tolgee.model.Prompt
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PromptModelAssembler : RepresentationModelAssemblerSupport<Prompt, PromptModel>(
  PromptController::class.java,
  PromptModel::class.java,
) {
  override fun toModel(entity: Prompt): PromptModel {
    return PromptModel(
      id = entity.id,
      name = entity.name,
      template = entity.template,
      projectId = entity.project.id,
    )
  }
}
