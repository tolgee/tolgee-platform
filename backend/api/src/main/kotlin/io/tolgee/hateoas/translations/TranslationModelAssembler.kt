package io.tolgee.hateoas.translations

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.model.translation.Translation
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationModelAssembler :
  RepresentationModelAssemblerSupport<Translation, TranslationModel>(
    TranslationsController::class.java,
    TranslationModel::class.java,
  ) {
  override fun toModel(entity: Translation): TranslationModel {
    return TranslationModel(
      id = entity.id,
      text = entity.text,
      state = entity.state,
      auto = entity.auto,
      mtProvider = entity.mtProvider,
      outdated = entity.outdated,
    )
  }
}
