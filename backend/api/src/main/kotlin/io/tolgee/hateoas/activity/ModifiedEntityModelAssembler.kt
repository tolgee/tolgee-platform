package io.tolgee.hateoas.activity

import io.tolgee.model.views.activity.ModifiedEntityView
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class ModifiedEntityModelAssembler : RepresentationModelAssembler<ModifiedEntityView, ModifiedEntityModel> {
  override fun toModel(view: ModifiedEntityView): ModifiedEntityModel {
    return ModifiedEntityModel(
      entityClass = view.entityClass,
      entityId = view.entityId,
      description = view.description,
      modifications = view.modifications,
      relations = view.describingRelations,
      exists = view.exists,
    )
  }
}
