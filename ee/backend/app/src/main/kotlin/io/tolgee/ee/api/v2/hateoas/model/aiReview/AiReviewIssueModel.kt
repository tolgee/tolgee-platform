package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.tolgee.model.aiReview.AiReviewIssue

data class AiReviewIssueModel(
  val severity: String,
  val category: String?,
  val message: String,
  val points: Int,
) {
  companion object {
    fun of(issue: AiReviewIssue) = AiReviewIssueModel(issue.severity, issue.category, issue.message, issue.points)
  }
}
