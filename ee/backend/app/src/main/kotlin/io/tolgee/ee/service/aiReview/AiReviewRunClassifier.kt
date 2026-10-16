package io.tolgee.ee.service.aiReview

import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.enums.aiReview.AiReviewRuleMode

object AiReviewRunClassifier {
  enum class Category {
    NO_RESULT,
    FAILED,
    PENDING,
    HAS_RESULT,
    AUTO_REVIEWED,
    HUMAN_REVIEWED,
  }

  enum class Kind {
    REVIEW,
    ALREADY_HAS_RESULT,
    HUMAN_REVIEWED,
    NO_RULE,
    NEVER,
    OPEN_QA_ISSUES,
    OUT_OF_SCOPE,
  }

  data class Candidate(
    val category: Category,
    val hasOpenQaIssues: Boolean,
    val scope: AiReviewRuleMatcher.KeyScope,
    val languageId: Long,
  )

  data class Context(
    val rules: List<AiReviewRule>,
    val qaCoveredLanguageIds: Set<Long>,
    val rereview: Boolean,
    val scopeRule: AiReviewRule? = null,
  ) {
    val activeRules = rules.filter { it.mode == AiReviewRuleMode.ACTIVE }
  }

  data class Decision(
    val kind: Kind,
    val rule: AiReviewRule? = null,
    val rereviewOfAutoReviewed: Boolean = false,
  )

  fun classify(
    candidate: Candidate,
    context: Context,
  ): Decision {
    val scopeRule = context.scopeRule
    if (scopeRule != null && !AiReviewRuleMatcher.matches(scopeRule, candidate.scope, candidate.languageId)) {
      return Decision(Kind.OUT_OF_SCOPE)
    }
    if (candidate.category == Category.HUMAN_REVIEWED) return Decision(Kind.HUMAN_REVIEWED)

    val match =
      when (val match = AiReviewRuleMatcher.match(context.rules, candidate.scope, candidate.languageId)) {
        is AiReviewRuleMatcher.Match.NoRule -> return Decision(Kind.NO_RULE)
        is AiReviewRuleMatcher.Match.Never -> return Decision(Kind.NEVER, match.rule)
        is AiReviewRuleMatcher.Match.Review -> match
      }

    return when (candidate.category) {
      Category.AUTO_REVIEWED -> classifyAutoReviewed(candidate, context)
      Category.HAS_RESULT -> {
        if (!context.rereview) return Decision(Kind.ALREADY_HAS_RESULT, match.rule)
        reviewUnlessOpenQaIssues(candidate, context, match.rule, rereviewOfAutoReviewed = false)
      }

      else -> reviewUnlessOpenQaIssues(candidate, context, match.rule, rereviewOfAutoReviewed = false)
    }
  }

  private fun classifyAutoReviewed(
    candidate: Candidate,
    context: Context,
  ): Decision {
    if (!context.rereview) return Decision(Kind.ALREADY_HAS_RESULT)
    val activeMatch =
      AiReviewRuleMatcher.match(context.activeRules, candidate.scope, candidate.languageId)
        as? AiReviewRuleMatcher.Match.Review
        ?: return Decision(Kind.ALREADY_HAS_RESULT)
    return reviewUnlessOpenQaIssues(candidate, context, activeMatch.rule, rereviewOfAutoReviewed = true)
  }

  private fun reviewUnlessOpenQaIssues(
    candidate: Candidate,
    context: Context,
    rule: AiReviewRule,
    rereviewOfAutoReviewed: Boolean,
  ): Decision {
    val qaCovered = candidate.languageId in context.qaCoveredLanguageIds
    if (rule.skipWithOpenQaIssues && qaCovered && candidate.hasOpenQaIssues) {
      return Decision(Kind.OPEN_QA_ISSUES, rule)
    }
    return Decision(Kind.REVIEW, rule, rereviewOfAutoReviewed)
  }
}
