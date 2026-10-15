package io.tolgee.ee.data.aiReview

import io.swagger.v3.oas.annotations.media.Schema

data class AiReviewRunCounts(
  @Schema(description = "Translations that will be reviewed")
  val willReview: Long = 0,
  @Schema(description = "Of willReview, translations matched only by Dry run rules (scored, state unchanged)")
  val dryRunOnly: Long = 0,
  @Schema(description = "Skipped: an AI review result for the current text exists")
  val alreadyHasResult: Long = 0,
  @Schema(description = "Skipped: reviewed by a human or AI review confirmed")
  val humanReviewed: Long = 0,
  @Schema(description = "Skipped: no enabled rule matches")
  val noRule: Long = 0,
  @Schema(description = "Skipped: the strictest matching rule sets the language to Never")
  val never: Long = 0,
  @Schema(description = "Skipped: open QA issues and the rule skips these")
  val openQaIssues: Long = 0,
)
