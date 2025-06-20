package io.tolgee.hateoas.translations

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.model.TranslationSuggestion
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationSuggestionModelAssembler :
  RepresentationModelAssemblerSupport<TranslationSuggestion, TranslationSuggestionModel>(
  TranslationsController::class.java,
  TranslationSuggestionModel::class.java,
) {
  override fun toModel(entity: TranslationSuggestion): TranslationSuggestionModel {
    return TranslationSuggestionModel(
      id = entity.id,
      languageId = entity.language!!.id,
      keyId = entity.key!!.id,
      translation = entity.translation,
      userId = entity.user!!.id,
      state = entity.state,
    )
  }
}
