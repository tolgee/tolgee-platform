package io.tolgee.hateoas.translations

import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.model.enums.TranslationSuggestionState
import org.springframework.hateoas.RepresentationModel

class TranslationSuggestionModel(
  val id: Long,
  val languageId: Long,
  val keyId: Long,
  val translation: String?,
  val state: TranslationSuggestionState,
  val author: SimpleUserAccountModel,
) : RepresentationModel<TranslationSuggestionModel>()
