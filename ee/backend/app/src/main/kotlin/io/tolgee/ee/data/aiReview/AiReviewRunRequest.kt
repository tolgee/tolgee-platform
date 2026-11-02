package io.tolgee.ee.data.aiReview

enum class AiReviewSkipReason {
  ALREADY_HAS_RESULT,
  ALREADY_QUEUED,
  CONFIRMED_BY_HUMAN,
  REJECTED_BY_HUMAN,
  HUMAN_REVIEWED,
  NO_RULE,
  NEVER,
  OPEN_QA_ISSUES,
  OUTDATED,
  EMPTY,
  NO_PERMISSION,
}

data class AiReviewRunRequest(
  val ruleId: Long? = null,
  val keyIds: List<Long>? = null,
  val translationIds: List<Long>? = null,
  val languageIds: List<Long> = emptyList(),
  val reReview: Boolean = false,
  val failedOnly: Boolean = false,
)

data class AiReviewRunPreviewModel(
  val toReview: Long,
  val dryRunOnly: Long,
  val skipped: Map<AiReviewSkipReason, Long>,
)
