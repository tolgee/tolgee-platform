package io.tolgee.hateoas.translations

import org.springframework.hateoas.RepresentationModel

class TranslationSuggestionModel (
  val languageId: Long,
  val translation: String?,
  val userId: Long,
) : RepresentationModel<TranslationSuggestionModel>()
