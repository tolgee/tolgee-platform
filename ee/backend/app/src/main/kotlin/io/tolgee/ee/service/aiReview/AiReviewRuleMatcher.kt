package io.tolgee.ee.service.aiReview

import io.tolgee.model.aiReview.AiReviewRule

object AiReviewRuleMatcher {
  data class KeyScope(
    val tags: Set<String>,
    val namespace: String?,
  )

  sealed interface Match {
    data object NoRule : Match

    data class Never(
      val rule: AiReviewRule,
    ) : Match

    data class Review(
      val rule: AiReviewRule,
      val threshold: Int,
    ) : Match
  }

  fun match(
    rules: Collection<AiReviewRule>,
    scope: KeyScope,
    languageId: Long,
  ): Match {
    val candidates = rules.filter { matches(it, scope, languageId) }.sortedBy { it.id }
    if (candidates.isEmpty()) return Match.NoRule

    val neverRule = candidates.firstOrNull { effectiveThreshold(it, languageId) == null }
    if (neverRule != null) return Match.Never(neverRule)

    val strictest = candidates.maxBy { effectiveThreshold(it, languageId)!! }
    return Match.Review(strictest, effectiveThreshold(strictest, languageId)!!)
  }

  fun matches(
    rule: AiReviewRule,
    scope: KeyScope,
    languageId: Long,
  ): Boolean {
    if (!rule.enabled) return false
    if (rule.languageIds.isNotEmpty() && languageId !in rule.languageIds) return false
    if (!matchesTagIn(rule.tagIn, scope.tags)) return false
    if (!matchesTagNotIn(rule.tagNotIn, scope.tags)) return false
    val namespace = scope.namespace ?: ""
    if (rule.namespaceIn.isNotEmpty() && namespace !in rule.namespaceIn) return false
    return namespace !in rule.namespaceNotIn
  }

  fun effectiveThreshold(
    rule: AiReviewRule,
    languageId: Long,
  ): Int? {
    val override = rule.languageThresholds.find { it.languageId == languageId } ?: return rule.threshold
    if (override.never) return null
    return override.threshold ?: rule.threshold
  }

  private fun matchesTagIn(
    tagIn: List<String>,
    tags: Set<String>,
  ): Boolean {
    if (tagIn.isEmpty()) return true
    if (tagIn.any { it.isNotEmpty() && it in tags }) return true
    return NO_TAG in tagIn && tags.isEmpty()
  }

  private fun matchesTagNotIn(
    tagNotIn: List<String>,
    tags: Set<String>,
  ): Boolean {
    if (tagNotIn.any { it.isNotEmpty() && it in tags }) return false
    return NO_TAG !in tagNotIn || tags.isNotEmpty()
  }

  private const val NO_TAG = ""
}
