package io.tolgee.hateoas.key

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.dtos.queryResults.KeyView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class KeyModelAssembler :
  RepresentationModelAssemblerSupport<KeyView, KeyModel>(
    TranslationsController::class.java,
    KeyModel::class.java,
  ) {
  @Suppress("UNCHECKED_CAST")
  override fun toModel(view: KeyView) =
    KeyModel(
      id = view.id,
      name = view.name,
      namespace = view.namespace,
      description = view.description,
      custom = view.custom as? Map<String, Any?>?,
    )
}
