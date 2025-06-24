package io.tolgee.hateoas.translations

import io.tolgee.model.enums.TranslationSuggestionState
import org.springframework.hateoas.RepresentationModel

class TranslationSuggestionSimpleModel(
  val id: Long,
  val translation: String?,
  val userId: Long,
  val state: TranslationSuggestionState,
) : RepresentationModel<TranslationSuggestionSimpleModel>()
