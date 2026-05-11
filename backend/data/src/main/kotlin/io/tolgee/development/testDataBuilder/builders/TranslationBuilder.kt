package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment

class TranslationBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<Translation, TranslationBuilder>() {
  class DATA {
    var comments = mutableListOf<TranslationCommentBuilder>()
    var qaIssues = mutableListOf<TranslationQaIssueBuilder>()
  }

  val data = DATA()

  override var self: Translation = Translation().apply { text = "What a text" }

  fun addComment(ft: FT<TranslationComment>) = addOperation(data.comments, ft)

  fun addQaIssue(ft: FT<TranslationQaIssue>) = addOperation(data.qaIssues, ft)
}
