package io.tolgee.model.aiReview

data class AiReviewLanguageThreshold(
  val languageId: Long = 0,
  val threshold: Int? = null,
  val never: Boolean = false,
)
