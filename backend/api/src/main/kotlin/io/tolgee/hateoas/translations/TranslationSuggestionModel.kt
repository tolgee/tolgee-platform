package io.tolgee.hateoas.translations

import io.tolgee.model.enums.TranslationSuggestionState
import org.springframework.hateoas.RepresentationModel

class TranslationSuggestionModel(
  val id: Long,
  val languageId: Long,
  val keyId: Long,
  val translation: String?,
  val userId: Long,
  val state: TranslationSuggestionState,
) : RepresentationModel<TranslationSuggestionModel>()
