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
  override fun toModel(entity: KeyWithBaseTranslationView): KeyWithBaseTranslationModel {
    return KeyWithBaseTranslationModel(entity.id, entity.name, entity.namespace, entity.baseTranslation)
  }
}
