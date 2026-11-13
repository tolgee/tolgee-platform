package io.tolgee.ee.service.aiReview.matching

/**
 * Must match the Criteria twin AiReviewRuleSqlPredicates in backend/data
 * (translationViewBuilder), including the tag, namespace and branch semantics.
 */
object RuleScopeMatcher {
  private const val WITHOUT_TAGS = ""

  fun matches(
    rule: RuleSnapshot,
    key: KeyScope,
    languageId: Long,
    branchingEnabled: Boolean,
  ): Boolean {
    if (!rule.enabled) return false
    if (rule.languageIds.isNotEmpty() && languageId !in rule.languageIds) return false
    if (rule.tagIn.isNotEmpty() && !matchesAnyTag(rule.tagIn, key.tags)) return false
    if (matchesAnyTag(rule.tagNotIn, key.tags)) return false
    if (rule.namespaceIn.isNotEmpty() && key.namespace !in rule.namespaceIn) return false
    if (key.namespace in rule.namespaceNotIn) return false
    if (!branchingEnabled) return true
    return matchesBranch(rule, key.branchName)
  }

  private fun matchesAnyTag(
    ruleTags: Set<String>,
    keyTags: Set<String>,
  ): Boolean {
    if (WITHOUT_TAGS in ruleTags && keyTags.isEmpty()) return true
    return keyTags.any { it in ruleTags }
  }

  fun branchCoverageChanges(
    rule: RuleSnapshot,
    oldName: String,
    newName: String,
  ): Boolean = rule.enabled && matchesBranch(rule, oldName) != matchesBranch(rule, newName)

  private fun matchesBranch(
    rule: RuleSnapshot,
    branchName: String,
  ): Boolean {
    if (rule.branchIn.isNotEmpty() && rule.branchIn.none { BranchPattern.matches(it, branchName) }) return false
    return rule.branchNotIn.none { BranchPattern.matches(it, branchName) }
  }
}
