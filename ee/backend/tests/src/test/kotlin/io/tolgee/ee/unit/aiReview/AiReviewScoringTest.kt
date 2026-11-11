package io.tolgee.ee.unit.aiReview

import io.tolgee.ee.service.aiReview.scoring.AiReviewScoring
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class AiReviewScoringTest {
  private fun issue(points: Int) = AiReviewIssue(severity = "major", message = "m", points = points)

  @Test
  fun `no issues score 100`() {
    AiReviewScoring.score(emptyList()).assert.isEqualTo(100)
  }

  @Test
  fun `score is 100 minus the points`() {
    AiReviewScoring.score(listOf(issue(35), issue(10))).assert.isEqualTo(55)
  }

  @Test
  fun `more than 100 points score 0`() {
    AiReviewScoring.score(listOf(issue(60), issue(60))).assert.isEqualTo(0)
  }

  @Test
  fun `active outcomes`() {
    AiReviewScoring.outcome(AiReviewRuleMode.ACTIVE, 85, 85).assert.isEqualTo(AiReviewStatus.APPROVED)
    AiReviewScoring.outcome(AiReviewRuleMode.ACTIVE, 84, 85).assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
  }

  @Test
  fun `score 0 is never approved`() {
    AiReviewScoring.outcome(AiReviewRuleMode.ACTIVE, 0, 0).assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
    AiReviewScoring.outcome(AiReviewRuleMode.SCORE_ONLY, 0, 0).assert.isEqualTo(AiReviewStatus.SCORE_ONLY_FAIL)
  }

  @Test
  fun `score-only outcomes`() {
    AiReviewScoring.outcome(AiReviewRuleMode.SCORE_ONLY, 90, 85).assert.isEqualTo(AiReviewStatus.SCORE_ONLY_PASS)
    AiReviewScoring.outcome(AiReviewRuleMode.SCORE_ONLY, 80, 85).assert.isEqualTo(AiReviewStatus.SCORE_ONLY_FAIL)
  }
}
