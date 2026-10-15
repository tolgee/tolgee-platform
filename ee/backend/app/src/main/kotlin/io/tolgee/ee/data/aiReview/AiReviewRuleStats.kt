package io.tolgee.ee.data.aiReview

data class AiReviewRuleStats(
  val scored: Long = 0,
  val wouldAutoReview: Long = 0,
  val autoReviewed: Long = 0,
  val rejected: Long = 0,
  val failed: Long = 0,
)
