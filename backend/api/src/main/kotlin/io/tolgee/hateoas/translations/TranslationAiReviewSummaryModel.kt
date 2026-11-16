package io.tolgee.hateoas.translations

import io.tolgee.model.enums.aiReview.AiReviewStatus

data class TranslationAiReviewSummaryModel(
  val status: AiReviewStatus,
  val score: Int?,
  val scoreOnlyRun: Boolean,
  val lastFailed: Boolean,
  val queued: Boolean,
)
