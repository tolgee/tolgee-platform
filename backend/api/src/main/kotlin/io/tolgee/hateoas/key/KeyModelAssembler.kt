package io.tolgee.hateoas.key

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.model.key.Key
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class KeyModelAssembler : RepresentationModelAssemblerSupport<Key, KeyModel>(
  TranslationsController::class.java,
  KeyModel::class.java,
) {
  override fun toModel(entity: Key) =
    KeyModel(
      id = entity.id,
      name = entity.name,
      namespace = entity.namespace?.name,
    )
}
