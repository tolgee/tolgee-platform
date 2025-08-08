package io.tolgee.hateoas.translations.suggestions

import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.model.enums.TranslationSuggestionState
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.Date

@Relation(collectionRelation = "suggestions", itemRelation = "suggestion")
class TranslationSuggestionModel(
  val id: Long,
  val languageId: Long,
  val keyId: Long,
  val translation: String?,
  val state: TranslationSuggestionState,
  val author: SimpleUserAccountModel,
  val isPlural: Boolean,
  val updatedAt: Date,
  val createdAt: Date,
) : RepresentationModel<TranslationSuggestionModel>()
