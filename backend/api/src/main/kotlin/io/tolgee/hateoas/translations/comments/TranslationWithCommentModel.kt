package io.tolgee.hateoas.translations.comments

import io.tolgee.hateoas.translations.TranslationModel

data class TranslationWithCommentModel(
  val translation: TranslationModel,
  val comment: TranslationCommentModel,
)
