package io.tolgee.ee.unit.aiReview

import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.ee.service.aiReview.matching.RuleSnapshot
import io.tolgee.ee.service.aiReview.matching.StrictestRuleSelector
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class StrictestRuleSelectorTest {
  private fun decide(
    vararg rules: RuleSnapshot,
    languageId: Long = DE,
    activeOnly: Boolean = false,
  ) = StrictestRuleSelector.decide(rules.toList(), key(), languageId, branchingEnabled = true, activeOnly = activeOnly)

  private fun RuleDecision.review() = this as RuleDecision.Review

  @Test
  fun `no matching rule gives NoRule`() {
    decide(rule(tagIn = setOf("legal"))).assert.isEqualTo(RuleDecision.NoRule)
  }

  @Test
  fun `any Never gives Never even with a lower threshold elsewhere`() {
    decide(rule(id = 1, threshold = 50), rule(id = 2, never = true)).assert.isEqualTo(RuleDecision.Never)
  }

  @Test
  fun `highest threshold wins`() {
    decide(rule(id = 1, threshold = 85), rule(id = 2, threshold = 92))
      .review()
      .rule.id.assert
      .isEqualTo(2)
  }

  @Test
  fun `on a tie Score only wins over Active`() {
    val decision =
      decide(
        rule(id = 1, threshold = 90, mode = AiReviewRuleMode.ACTIVE),
        rule(id = 2, threshold = 90, mode = AiReviewRuleMode.SCORE_ONLY),
      )
    decision
      .review()
      .rule.id.assert
      .isEqualTo(2)
  }

  @Test
  fun `on a tie of two Active rules the lowest id wins`() {
    decide(rule(id = 7, threshold = 90), rule(id = 3, threshold = 90))
      .review()
      .rule.id.assert
      .isEqualTo(3)
  }

  @Test
  fun `review threshold is the effective value`() {
    decide(rule(threshold = 77))
      .review()
      .threshold.assert
      .isEqualTo(77)
  }

  @Test
  fun `activeOnly ignores a stricter Score only rule`() {
    val decision =
      decide(
        rule(id = 1, threshold = 85, mode = AiReviewRuleMode.ACTIVE),
        rule(id = 2, threshold = 95, mode = AiReviewRuleMode.SCORE_ONLY),
        activeOnly = true,
      )
    decision
      .review()
      .rule.id.assert
      .isEqualTo(1)
  }

  @Test
  fun `activeOnly with only Score only rules gives NoRule`() {
    decide(rule(mode = AiReviewRuleMode.SCORE_ONLY), activeOnly = true).assert.isEqualTo(RuleDecision.NoRule)
  }

  @Test
  fun `Never of a Score only rule blocks even with activeOnly`() {
    val decision =
      decide(
        rule(id = 1, threshold = 85, mode = AiReviewRuleMode.ACTIVE),
        rule(id = 2, never = true, mode = AiReviewRuleMode.SCORE_ONLY),
        activeOnly = true,
      )
    decision.assert.isEqualTo(RuleDecision.Never)
  }
}
