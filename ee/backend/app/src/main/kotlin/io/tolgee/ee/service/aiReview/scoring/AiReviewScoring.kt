package io.tolgee.ee.service.aiReview.scoring

import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus

object AiReviewScoring {
  private const val MAX_SCORE = 100

  fun score(issues: List<AiReviewIssue>): Int =
    (MAX_SCORE - issues.sumOf { it.points.toLong() }).coerceAtLeast(0).toInt()

  fun outcome(
    mode: AiReviewRuleMode,
    score: Int,
    threshold: Int,
  ): AiReviewStatus {
    val passes = score > 0 && score >= threshold
    val active = mode == AiReviewRuleMode.ACTIVE
    return when {
      active && passes -> AiReviewStatus.APPROVED
      active -> AiReviewStatus.BELOW_THRESHOLD
      passes -> AiReviewStatus.SCORE_ONLY_PASS
      else -> AiReviewStatus.SCORE_ONLY_FAIL
    }
  }
}
