package io.tolgee.model.enums.aiReview

enum class AiReviewIssueSeverity(
  val defaultPoints: Int,
) {
  CRITICAL(75),
  MAJOR(25),
  MINOR(5),
}
