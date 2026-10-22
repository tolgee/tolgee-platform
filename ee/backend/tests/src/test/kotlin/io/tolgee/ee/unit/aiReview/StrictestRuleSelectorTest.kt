package io.tolgee.ee.unit.aiReview

import io.tolgee.ee.service.aiReview.matching.EffectiveValue
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
  fun `language override beats the rule value for that language only`() {
    val ruleWithOverride = rule(id = 1, threshold = 92, overrides = mapOf(DE to EffectiveValue(95, false)))
    decide(ruleWithOverride, languageId = DE)
      .review()
      .threshold.assert
      .isEqualTo(95)
    decide(ruleWithOverride, languageId = FR)
      .review()
      .threshold.assert
      .isEqualTo(92)
  }

  @Test
  fun `highest threshold wins`() {
    decide(rule(id = 1, threshold = 85), rule(id = 2, threshold = 92))
      .review()
      .rule.id.assert
      .isEqualTo(2)
  }

  @Test
  fun `on a tie Dry run wins over Active`() {
    val decision =
      decide(
        rule(id = 1, threshold = 90, mode = AiReviewRuleMode.ACTIVE),
        rule(id = 2, threshold = 90, mode = AiReviewRuleMode.DRY_RUN),
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
  fun `override never applies only to its language`() {
    val ruleWithNever = rule(id = 1, overrides = mapOf(AR to EffectiveValue(85, true)))
    decide(ruleWithNever, languageId = AR).assert.isEqualTo(RuleDecision.Never)
    decide(ruleWithNever, languageId = DE).assert.isInstanceOf(RuleDecision.Review::class.java)
  }

  @Test
  fun `review threshold is the effective value`() {
    decide(rule(threshold = 77))
      .review()
      .threshold.assert
      .isEqualTo(77)
  }

  @Test
  fun `activeOnly ignores a stricter Dry run rule`() {
    val decision =
      decide(
        rule(id = 1, threshold = 85, mode = AiReviewRuleMode.ACTIVE),
        rule(id = 2, threshold = 95, mode = AiReviewRuleMode.DRY_RUN),
        activeOnly = true,
      )
    decision
      .review()
      .rule.id.assert
      .isEqualTo(1)
  }

  @Test
  fun `activeOnly with only Dry run rules gives NoRule`() {
    decide(rule(mode = AiReviewRuleMode.DRY_RUN), activeOnly = true).assert.isEqualTo(RuleDecision.NoRule)
  }

  @Test
  fun `Never of a Dry run rule blocks even with activeOnly`() {
    val decision =
      decide(
        rule(id = 1, threshold = 85, mode = AiReviewRuleMode.ACTIVE),
        rule(id = 2, never = true, mode = AiReviewRuleMode.DRY_RUN),
        activeOnly = true,
      )
    decision.assert.isEqualTo(RuleDecision.Never)
  }

  @Test
  fun `threshold override makes an exception to a rule-level Never`() {
    val neverRule = rule(never = true, overrides = mapOf(DE to EffectiveValue(98, false)))
    decide(neverRule, languageId = DE)
      .review()
      .threshold.assert
      .isEqualTo(98)
    decide(neverRule, languageId = FR).assert.isEqualTo(RuleDecision.Never)
  }
}
