package io.tolgee.api.v2.hateoas.invitation

import io.tolgee.api.v2.controllers.TagsController
import io.tolgee.model.key.Tag
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TagModelAssembler :
  RepresentationModelAssemblerSupport<Tag, TagModel>(
    TagsController::class.java,
    TagModel::class.java,
  ) {
  override fun toModel(entity: Tag): TagModel {
    return TagModel(entity.id, entity.name)
  }
}
