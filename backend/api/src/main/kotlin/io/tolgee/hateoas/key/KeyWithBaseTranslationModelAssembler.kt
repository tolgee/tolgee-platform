package io.tolgee.hateoas.key

import io.tolgee.service.key.KeyWithBaseTranslationView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class KeyWithBaseTranslationModelAssembler :
  RepresentationModelAssemblerSupport<KeyWithBaseTranslationView, KeyWithBaseTranslationModel>(
    KeyWithBaseTranslationView::class.java,
    KeyWithBaseTranslationModel::class.java,
  ) {
  override fun toModel(view: KeyWithBaseTranslationView): KeyWithBaseTranslationModel {
    return KeyWithBaseTranslationModel(
      id = view.id,
      name = view.name,
      namespace = view.namespace,
      baseTranslation = view.baseTranslation,
    )
  }
}
