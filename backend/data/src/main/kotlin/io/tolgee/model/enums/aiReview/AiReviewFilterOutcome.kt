package io.tolgee.model.enums.aiReview

enum class AiReviewFilterOutcome {
  REVIEWED_BY_AI,
  BELOW_THRESHOLD,
  SCORE_ONLY_PASS,
  SCORE_ONLY_FAIL,
  FAILED,
  OVERRIDDEN,
  NOT_REVIEWED_YET,
}
