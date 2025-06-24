package io.tolgee.hateoas.translations

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.model.views.TranslationSuggestionView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationSuggestionSimpleModelAssembler :
  RepresentationModelAssemblerSupport<TranslationSuggestionView, TranslationSuggestionSimpleModel>(
  TranslationsController::class.java,
  TranslationSuggestionSimpleModel::class.java,
) {
  override fun toModel(entity: TranslationSuggestionView): TranslationSuggestionSimpleModel {
    return TranslationSuggestionSimpleModel(
      id = entity.id,
      translation = entity.translation,
      userId = entity.userId,
      state = entity.state,
    )
  }
}
