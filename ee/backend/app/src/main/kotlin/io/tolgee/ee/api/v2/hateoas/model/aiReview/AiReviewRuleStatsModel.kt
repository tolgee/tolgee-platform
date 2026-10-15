package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.swagger.v3.oas.annotations.media.Schema

data class AiReviewRuleStatsModel(
  @Schema(description = "Dry run results (scored translations)")
  val scored: Long = 0,
  @Schema(description = "Dry run results that would be auto-reviewed")
  val wouldAutoReview: Long = 0,
  @Schema(description = "Translations auto-reviewed by the rule in active mode (including later confirmed or rejected)")
  val autoReviewed: Long = 0,
  @Schema(description = "Auto-reviewed translations rejected by a user")
  val rejected: Long = 0,
  @Schema(description = "Failed reviews of translations that are still in the translated state")
  val failed: Long = 0,
)
