package io.tolgee.ee.service.aiReview

data class AiReviewRuleChangedEvent(
  val projectId: Long,
  val ruleId: Long,
)
