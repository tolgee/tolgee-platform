package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.translation.TranslationComment

class TranslationCommentBuilder(
  val translationBuilder: TranslationBuilder
) : BaseEntityDataBuilder<TranslationComment, TranslationCommentBuilder>() {
  override var self: TranslationComment = TranslationComment(
    translation = translationBuilder.self,
  ).also { comment ->
    translationBuilder.self.key.project?.userOwner?.let {
      comment.author = it
    }
  }
}
