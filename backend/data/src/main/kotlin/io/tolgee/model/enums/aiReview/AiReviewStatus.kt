package io.tolgee.model.enums.aiReview

enum class AiReviewStatus {
  PENDING,
  APPROVED,
  BELOW_THRESHOLD,
  SCORE_ONLY_PASS,
  SCORE_ONLY_FAIL,
  FAILED,
  SKIPPED_QA_ISSUES,
  OVERRIDDEN,
}
