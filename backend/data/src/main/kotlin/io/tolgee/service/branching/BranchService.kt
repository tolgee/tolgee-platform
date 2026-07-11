package io.tolgee.service.branching

import io.tolgee.dtos.queryResults.branching.BranchMergeChangeView
import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.dtos.request.branching.DryRunMergeBranchRequest
import io.tolgee.dtos.request.branching.ResolveAllBranchMergeConflictsRequest
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.enums.BranchKeyMergeChangeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BranchService {
  fun getBranches(
    projectId: Long,
    page: Pageable,
    search: String? = null,
  ): Page<Branch>

  fun getActiveBranch(
    projectId: Long,
    branchName: String,
  ): Branch

  fun getActiveOrDefault(
    projectId: Long,
    branchName: String?,
  ): Branch?

  fun getActiveNonDefaultBranch(
    projectId: Long,
    branchName: String?,
  ): Branch?

  fun getDefaultBranch(projectId: Long): Branch?

  fun renameBranch(
    projectId: Long,
    branchId: Long,
    name: String,
  ): Branch

  fun setProtected(
    projectId: Long,
    branchId: Long,
    isProtected: Boolean,
  ): Branch

  fun createBranch(
    projectId: Long,
    name: String,
    originBranchId: Long,
    author: UserAccount,
  ): Branch

  fun deleteBranch(
    projectId: Long,
    branchId: Long,
  )

  fun dryRunMerge(
    projectId: Long,
    request: DryRunMergeBranchRequest,
  ): BranchMerge

  fun getBranchMergeView(
    projectId: Long,
    mergeId: Long,
  ): BranchMergeView

  fun getBranchMerges(
    projectId: Long,
    pageable: Pageable,
  ): Page<BranchMergeView>

  fun refreshMerge(
    projectId: Long,
    mergeId: Long,
  ): BranchMergeView

  fun getBranchMergeConflicts(
    projectId: Long,
    branchMergeId: Long,
    pageable: Pageable,
  ): Page<BranchMergeConflictView>

  fun getBranchMergeChanges(
    projectId: Long,
    branchMergeId: Long,
    type: BranchKeyMergeChangeType?,
    pageable: Pageable,
  ): Page<BranchMergeChangeView>

  fun getBranchMergeChange(
    projectId: Long,
    branchMergeId: Long,
    changeId: Long,
  ): BranchMergeChangeView

  fun resolveConflict(
    projectId: Long,
    mergeId: Long,
    request: ResolveBranchMergeConflictRequest,
  )

  fun resolveAllConflicts(
    projectId: Long,
    mergeId: Long,
    request: ResolveAllBranchMergeConflictsRequest,
  )

  fun applyMerge(
    projectId: Long,
    mergeId: Long,
    deleteBranch: Boolean? = true,
  )

  fun deleteMerge(
    projectId: Long,
    mergeId: Long,
  )

  fun enableBranchingOnProject(projectId: Long)

  fun deleteAllByProjectId(projectId: Long)

  fun deleteBranchMergeChangesByKeyIds(keyIds: Set<Long>)
}
