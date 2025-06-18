package io.tolgee.hateoas.label

import io.tolgee.api.v2.controllers.LabelsController
import io.tolgee.model.translation.Label
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LabelModelAssembler : RepresentationModelAssemblerSupport<Label, LabelModel>(
  LabelsController::class.java,
  LabelModel::class.java,
) {
  override fun toModel(entity: Label): LabelModel {
    return LabelModel(entity.id, entity.name, entity.color, entity.description)
  }
}
