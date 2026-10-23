package io.tolgee.ee.service.aiReview

import io.tolgee.ee.service.aiReview.matching.EffectiveValue
import io.tolgee.ee.service.aiReview.matching.RuleSnapshot
import io.tolgee.model.aiReview.AiReviewRule

fun AiReviewRule.toSnapshot() =
  RuleSnapshot(
    id = id,
    enabled = enabled,
    tagIn = tagIn.toSet(),
    tagNotIn = tagNotIn.toSet(),
    namespaceIn = namespaceIn.toSet(),
    namespaceNotIn = namespaceNotIn.toSet(),
    languageIds = languages.map { it.id }.toSet(),
    branchIn = branchIn.toList(),
    branchNotIn = branchNotIn.toList(),
    value = EffectiveValue(threshold = threshold, never = never),
    overrides = overrides.associate { it.language.id to EffectiveValue(it.threshold, it.never) },
    mode = mode,
    promptId = prompt?.id,
    skipOpenQaIssues = skipOpenQaIssues,
  )
