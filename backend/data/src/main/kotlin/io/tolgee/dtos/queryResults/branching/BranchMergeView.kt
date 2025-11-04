package io.tolgee.dtos.queryResults.branching

import io.tolgee.model.branching.Branch

data class BranchMergeView(
  val id: Long,
  val name: String,
  val sourceBranch: Branch,
  val targetBranch: Branch,
  val revisionsMatch: Boolean,
  val keyAdditionsCount: Long,
  val keyDeletionsCount: Long,
  val keyModificationsCount: Long,
  val keyUnresolvedConflictsCount: Long,
  val keyResolvedConflictsCount: Long,
)
