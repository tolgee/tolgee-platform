package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.aiReview.TranslationAiReview

class TranslationAiReviewBuilder(
  val translationBuilder: TranslationBuilder,
) : EntityDataBuilder<TranslationAiReview, TranslationAiReviewBuilder> {
  override var self: TranslationAiReview = TranslationAiReview(translation = translationBuilder.self)
}
