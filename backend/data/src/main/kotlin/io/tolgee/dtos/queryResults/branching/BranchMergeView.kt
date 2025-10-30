package io.tolgee.dtos.queryResults.branching

import io.tolgee.model.branching.Branch

data class BranchMergeView(
  val id: Long,
  val sourceBranch: Branch,
  val targetBranch: Branch,
  val keyAdditionsCount: Long,
  val keyDeletionsCount: Long,
  val keyModificationsCount: Long,
  val keyConflictsCount: Long,
)
