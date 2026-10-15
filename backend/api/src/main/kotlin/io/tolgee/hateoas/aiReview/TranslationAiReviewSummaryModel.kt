package io.tolgee.hateoas.aiReview

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.aiReview.AiReviewOutcome

data class TranslationAiReviewSummaryModel(
  @Schema(description = "Result of the AI review of the current translation text")
  val outcome: AiReviewOutcome,
  @Schema(description = "Score 0-100 computed from the issues the reviewer reported")
  val score: Int?,
  @Schema(description = "Threshold the score was compared with. Null when the rule never auto-reviews this language.")
  val threshold: Int?,
  @Schema(description = "Whether the rule ran in dry run mode (the state was not changed)")
  val dryRun: Boolean,
)
