package io.tolgee.api.v2.hateoas.translations.comments

import io.tolgee.api.v2.hateoas.translations.TranslationModel

data class TranslationWithCommentModel(
  val translation: TranslationModel,
  val comment: TranslationCommentModel
)
