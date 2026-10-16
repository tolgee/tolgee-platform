package io.tolgee.model.aiReview

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.tolgee.model.enums.aiReview.AiReviewIssueSeverity

@JsonIgnoreProperties(ignoreUnknown = true)
data class AiReviewIssue(
  val severity: AiReviewIssueSeverity = AiReviewIssueSeverity.MINOR,
  val category: String = "other",
  val message: String = "",
  val points: Int = 0,
)
