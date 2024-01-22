package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.translation.TranslationComment

class TranslationCommentBuilder(
  private val translationBuilder: TranslationBuilder,
) : BaseEntityDataBuilder<TranslationComment, TranslationCommentBuilder>() {
  override var self: TranslationComment =
    TranslationComment(
      translation = translationBuilder.self,
    ).also { comment ->
      translationBuilder.projectBuilder.onlyUser?.let {
        comment.author = it
      }
    }
}
