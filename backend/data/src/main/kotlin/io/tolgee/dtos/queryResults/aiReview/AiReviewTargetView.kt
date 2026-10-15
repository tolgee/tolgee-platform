package io.tolgee.dtos.queryResults.aiReview

data class AiReviewTargetView(
  val translationId: Long,
  val keyId: Long,
  val languageId: Long,
  val qaChecksStale: Boolean,
)
