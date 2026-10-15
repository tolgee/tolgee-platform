package io.tolgee.dtos.queryResults.aiReview

data class AiReviewTextView(
  val translationId: Long,
  val keyId: Long,
  val languageId: Long,
  val text: String?,
)
