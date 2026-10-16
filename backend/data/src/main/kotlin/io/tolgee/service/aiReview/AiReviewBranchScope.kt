package io.tolgee.service.aiReview

import io.tolgee.model.aiReview.AiReviewRule
import java.util.concurrent.ConcurrentHashMap

fun AiReviewRule.appliesToBranch(branch: String): Boolean {
  if (branchIn.isNotEmpty() && branchIn.none { matchesBranchPattern(it, branch) }) return false
  return branchNotIn.none { matchesBranchPattern(it, branch) }
}

private val branchPatternRegexes = ConcurrentHashMap<String, Regex>()

fun matchesBranchPattern(
  pattern: String,
  branch: String,
): Boolean =
  branchPatternRegexes
    .computeIfAbsent(pattern) { Regex(it.split("*").joinToString(".*") { part -> Regex.escape(part) }) }
    .matches(branch)
