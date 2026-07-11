package io.tolgee.hateoas.translations.suggestions

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.TranslationSuggestion
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import java.util.Date

@Component
class TranslationSuggestionModelAssembler(
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
) : RepresentationModelAssemblerSupport<TranslationSuggestion, TranslationSuggestionModel>(
    TranslationsController::class.java,
    TranslationSuggestionModel::class.java,
  ) {
  override fun toModel(entity: TranslationSuggestion): TranslationSuggestionModel {
    return TranslationSuggestionModel(
      id = entity.id,
      languageId = entity.language!!.id,
      keyId = entity.key!!.id,
      translation = entity.translation,
      author = simpleUserAccountModelAssembler.toModel(entity.author!!),
      state = entity.state,
      updatedAt = entity.updatedAt ?: Date(),
      createdAt = entity.createdAt ?: Date(),
      isPlural = entity.isPlural,
    )
  }
}
