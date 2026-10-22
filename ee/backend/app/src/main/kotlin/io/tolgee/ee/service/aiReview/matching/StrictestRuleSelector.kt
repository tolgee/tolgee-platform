package io.tolgee.ee.service.aiReview.matching

import io.tolgee.model.enums.aiReview.AiReviewRuleMode

object StrictestRuleSelector {
  fun decide(
    rules: List<RuleSnapshot>,
    key: KeyScope,
    languageId: Long,
    branchingEnabled: Boolean,
    activeOnly: Boolean = false,
  ): RuleDecision {
    val matching = rules.filter { RuleScopeMatcher.matches(it, key, languageId, branchingEnabled) }
    if (matching.any { it.valueFor(languageId).never }) return RuleDecision.Never

    val candidates = matching.filter { !activeOnly || it.mode == AiReviewRuleMode.ACTIVE }
    if (candidates.isEmpty()) return RuleDecision.NoRule

    val winner =
      candidates.minWith(
        compareByDescending<RuleSnapshot> { it.valueFor(languageId).threshold }
          .thenBy { tieOrder(it.mode) }
          .thenBy { it.id },
      )
    return RuleDecision.Review(winner, winner.valueFor(languageId).threshold)
  }

  private fun tieOrder(mode: AiReviewRuleMode): Int =
    when (mode) {
      AiReviewRuleMode.DRY_RUN -> 0
      AiReviewRuleMode.ACTIVE -> 1
    }
}
