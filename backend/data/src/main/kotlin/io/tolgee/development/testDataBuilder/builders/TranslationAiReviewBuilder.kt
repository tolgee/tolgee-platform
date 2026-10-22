package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.aiReview.TranslationAiReview

class TranslationAiReviewBuilder(
  val translationBuilder: TranslationBuilder,
) : BaseEntityDataBuilder<TranslationAiReview, TranslationAiReviewBuilder>() {
  override var self: TranslationAiReview = TranslationAiReview(translation = translationBuilder.self)
}
