package io.tolgee.ee.service.aiReview

import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.translation.Translation

data class AiReviewUpdatedEvent(
  val projectId: Long,
  val translationId: Long,
  val keyId: Long,
  val languageTag: String,
  val state: TranslationState,
  val outcome: AiReviewOutcome,
  val score: Int?,
  val threshold: Int?,
  val dryRun: Boolean,
) {
  companion object {
    fun of(
      projectId: Long,
      translation: Translation,
      review: TranslationAiReview,
    ) = AiReviewUpdatedEvent(
      projectId = projectId,
      translationId = translation.id,
      keyId = translation.key.id,
      languageTag = translation.language.tag,
      state = translation.state,
      outcome = review.outcome,
      score = review.score,
      threshold = review.threshold,
      dryRun = review.dryRun,
    )
  }
}
