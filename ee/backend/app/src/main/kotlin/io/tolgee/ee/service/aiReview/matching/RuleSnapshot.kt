package io.tolgee.ee.service.aiReview.matching

import io.tolgee.model.enums.aiReview.AiReviewRuleMode

data class EffectiveValue(
  val threshold: Int,
  val never: Boolean,
)

data class RuleSnapshot(
  val id: Long,
  val enabled: Boolean,
  val tagIn: Set<String>,
  val tagNotIn: Set<String>,
  val namespaceIn: Set<String>,
  val namespaceNotIn: Set<String>,
  val languageIds: Set<Long>,
  val branchIn: List<String>,
  val branchNotIn: List<String>,
  val value: EffectiveValue,
  val overrides: Map<Long, EffectiveValue>,
  val mode: AiReviewRuleMode,
  val promptId: Long?,
  val skipOpenQaIssues: Boolean,
) {
  fun valueFor(languageId: Long): EffectiveValue = overrides[languageId] ?: value
}

data class KeyScope(
  val keyId: Long,
  val namespace: String,
  val branchName: String,
  val tags: Set<String>,
)

sealed interface RuleDecision {
  data object NoRule : RuleDecision

  data object Never : RuleDecision

  data class Review(
    val rule: RuleSnapshot,
    val threshold: Int,
  ) : RuleDecision
}
