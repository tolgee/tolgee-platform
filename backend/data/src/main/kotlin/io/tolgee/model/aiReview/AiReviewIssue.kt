package io.tolgee.model.aiReview

data class AiReviewIssue(
  var severity: String = "",
  var category: String? = null,
  var message: String = "",
  var points: Int = 0,
)
