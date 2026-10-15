package io.tolgee.dtos.queryResults.aiReview

import io.tolgee.model.enums.aiReview.AiReviewOutcome

data class AiReviewRuleStatsView(
  val ruleId: Long,
  val dryRun: Boolean,
  val outcome: AiReviewOutcome,
  val count: Long,
)
