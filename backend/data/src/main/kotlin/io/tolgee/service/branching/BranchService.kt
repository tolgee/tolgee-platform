package io.tolgee.service.branching

import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.dtos.request.branching.DryRunMergeBranchRequest
import io.tolgee.model.branching.Branch
import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.model.branching.BranchMerge
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BranchService {
  fun getAllBranches(projectId: Long, page: Pageable, search: String? = null): Page<Branch>
  fun getBranch(projectId: Long, branchId: Long): Branch
  fun createBranch(projectId: Long, name: String, originBranchId: Long): Branch
  fun deleteBranch(projectId: Long, branchId: Long)
  fun dryRunMergeBranch(projectId: Long, request: DryRunMergeBranchRequest): BranchMerge
  fun getBranchMergeView(projectId: Long, mergeId: Long): BranchMergeView
  fun getBranchMerges(projectId: Long, pageable: Pageable): Page<BranchMergeView>
  fun getBranchMergeConflicts(
    projectId: Long,
    branchMergeId: Long,
    pageable: Pageable
  ): Page<BranchMergeConflictView>
  fun resolveConflict(projectId: Long, mergeId: Long, request: ResolveBranchMergeConflictRequest)
  fun applyMerge(projectId: Long, mergeId: Long)
}
