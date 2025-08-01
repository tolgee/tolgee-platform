package io.tolgee.hateoas.translations.suggestions

import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.model.enums.TranslationSuggestionState
import org.springframework.hateoas.RepresentationModel

class TranslationSuggestionSimpleModel(
  val id: Long,
  val translation: String?,
  val isPlural: Boolean,
  val author: SimpleUserAccountModel,
  val state: TranslationSuggestionState,
) : RepresentationModel<TranslationSuggestionSimpleModel>()
