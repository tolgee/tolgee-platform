package io.tolgee.hateoas.label

import io.tolgee.model.translation.Label
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class LabelModelAssembler : RepresentationModelAssembler<Label, LabelModel> {
  override fun toModel(entity: Label): LabelModel {
    return LabelModel(entity.id, entity.name, entity.color, entity.description)
  }
}
