package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.qa.TranslationQaIssue

class TranslationQaIssueBuilder(
  private val translationBuilder: TranslationBuilder,
) : BaseEntityDataBuilder<TranslationQaIssue, TranslationQaIssueBuilder>() {
  override var self: TranslationQaIssue =
    TranslationQaIssue(
      translation = translationBuilder.self,
    )
}
