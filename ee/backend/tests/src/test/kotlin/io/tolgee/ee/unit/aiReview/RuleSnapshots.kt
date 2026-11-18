package io.tolgee.ee.unit.aiReview

import io.tolgee.ee.service.aiReview.matching.KeyScope
import io.tolgee.ee.service.aiReview.matching.RuleSnapshot
import io.tolgee.model.enums.aiReview.AiReviewRuleMode

const val DE = 2L
const val FR = 3L
const val AR = 4L

fun rule(
  id: Long = 1,
  enabled: Boolean = true,
  tagIn: Set<String> = emptySet(),
  tagNotIn: Set<String> = emptySet(),
  namespaceIn: Set<String> = emptySet(),
  namespaceNotIn: Set<String> = emptySet(),
  languageIds: Set<Long> = emptySet(),
  branchIn: List<String> = emptyList(),
  branchNotIn: List<String> = emptyList(),
  threshold: Int = 85,
  never: Boolean = false,
  mode: AiReviewRuleMode = AiReviewRuleMode.ACTIVE,
) = RuleSnapshot(
  id = id,
  enabled = enabled,
  tagIn = tagIn,
  tagNotIn = tagNotIn,
  namespaceIn = namespaceIn,
  namespaceNotIn = namespaceNotIn,
  languageIds = languageIds,
  branchIn = branchIn,
  branchNotIn = branchNotIn,
  threshold = threshold,
  never = never,
  mode = mode,
  promptId = null,
  skipOpenQaIssues = true,
)

fun key(
  tags: Set<String> = setOf("ui"),
  namespace: String = "",
  branchName: String = "main",
) = KeyScope(keyId = 10, namespace = namespace, branchName = branchName, tags = tags)
