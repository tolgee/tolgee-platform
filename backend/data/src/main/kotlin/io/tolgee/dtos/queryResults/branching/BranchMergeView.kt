package io.tolgee.dtos.queryResults.branching

import io.tolgee.model.branching.Branch
import java.util.Date

data class BranchMergeView(
  val id: Long,
  val sourceBranch: Branch,
  val targetBranch: Branch,
  val mergedAt: Date?,
  val revisionsMatch: Boolean,
  val keyAdditionsCount: Long,
  val keyDeletionsCount: Long,
  val keyModificationsCount: Long,
  val keyUnresolvedConflictsCount: Long,
  val keyResolvedConflictsCount: Long,
  val uncompletedTasksCount: Long,
)
