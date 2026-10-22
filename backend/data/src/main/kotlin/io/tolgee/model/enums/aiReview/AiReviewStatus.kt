package io.tolgee.model.enums.aiReview

enum class AiReviewStatus {
  PENDING,
  APPROVED,
  BELOW_THRESHOLD,
  DRY_RUN_PASS,
  DRY_RUN_FAIL,
  FAILED,
  SKIPPED_QA_ISSUES,
  CONFIRMED,
  REJECTED,
}
