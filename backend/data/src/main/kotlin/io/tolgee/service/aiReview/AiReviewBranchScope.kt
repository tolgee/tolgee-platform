package io.tolgee.service.aiReview

import io.tolgee.model.aiReview.AiReviewRule

fun AiReviewRule.appliesToBranch(branch: String): Boolean {
  if (branchIn.isNotEmpty() && branchIn.none { matchesBranchPattern(it, branch) }) return false
  return branchNotIn.none { matchesBranchPattern(it, branch) }
}

fun matchesBranchPattern(
  pattern: String,
  branch: String,
): Boolean = Regex(pattern.split("*").joinToString(".*") { Regex.escape(it) }).matches(branch)
