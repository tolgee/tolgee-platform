package io.tolgee.hateoas.translations

import io.tolgee.model.enums.aiReview.AiReviewStatus

data class TranslationAiReviewSummaryModel(
  val status: AiReviewStatus,
  val score: Int?,
  val dryRun: Boolean,
  val lastFailed: Boolean,
)
