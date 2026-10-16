package io.tolgee.dtos.queryResults.aiReview

import io.tolgee.model.enums.aiReview.AiReviewOutcome

data class TranslationAiReviewView(
  val translationId: Long,
  val outcome: AiReviewOutcome,
  val score: Int?,
  val threshold: Int?,
  val dryRun: Boolean,
  val textHash: String,
)
