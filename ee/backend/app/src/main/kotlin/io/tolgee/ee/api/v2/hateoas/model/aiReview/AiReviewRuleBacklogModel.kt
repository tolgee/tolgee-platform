package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.swagger.v3.oas.annotations.media.Schema

data class AiReviewRuleBacklogModel(
  val ruleId: Long,
  @Schema(description = "Existing translations the rule covers that a run would review now")
  val notReviewed: Long,
)

data class AiReviewRuleBacklogsModel(
  @Schema(description = "One entry per enabled rule")
  val backlogs: List<AiReviewRuleBacklogModel>,
)
