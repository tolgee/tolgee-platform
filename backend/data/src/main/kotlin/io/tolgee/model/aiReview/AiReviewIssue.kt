package io.tolgee.model.aiReview

import io.tolgee.model.enums.aiReview.AiReviewIssueSeverity

data class AiReviewIssue(
  val severity: AiReviewIssueSeverity = AiReviewIssueSeverity.MINOR,
  val category: String = "other",
  val message: String = "",
  val suggestion: String? = null,
  val points: Int = 0,
)
