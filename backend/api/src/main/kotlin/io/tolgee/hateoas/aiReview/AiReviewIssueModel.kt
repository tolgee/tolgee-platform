package io.tolgee.hateoas.aiReview

import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.enums.aiReview.AiReviewIssueSeverity

data class AiReviewIssueModel(
  val severity: AiReviewIssueSeverity,
  val category: String,
  val message: String,
  val points: Int,
) {
  companion object {
    fun of(issue: AiReviewIssue) =
      AiReviewIssueModel(
        severity = issue.severity,
        category = issue.category,
        message = issue.message,
        points = issue.points,
      )
  }
}
