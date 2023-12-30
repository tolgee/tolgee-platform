package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment

class TranslationBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<Translation, TranslationBuilder>() {
  class DATA {
    var comments = mutableListOf<TranslationCommentBuilder>()
  }

  val data = DATA()

  override var self: Translation = Translation().apply { text = "What a text" }

  fun addComment(ft: FT<TranslationComment>) = addOperation(data.comments, ft)
}
