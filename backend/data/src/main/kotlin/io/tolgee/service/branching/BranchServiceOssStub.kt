package io.tolgee.service.branching

import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.dtos.request.branching.DryRunMergeBranchRequest
import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class BranchServiceOssStub : BranchService {
  override fun getAllBranches(projectId: Long, page: Pageable, search: String?): Page<Branch> {
    return Page.empty()
  }

  override fun getBranch(projectId: Long, branchId: Long): Branch {
    throw UnsupportedOperationException()
  }

  override fun createBranch(projectId: Long, name: String, originBranchId: Long): Branch {
    throw UnsupportedOperationException()
  }

  override fun deleteBranch(projectId: Long, branchId: Long) {
    throw UnsupportedOperationException()
  }

  override fun dryRunMergeBranch(projectId: Long, request: DryRunMergeBranchRequest): BranchMerge {
    throw UnsupportedOperationException()
  }

  override fun getBranchMergeView(projectId: Long, mergeId: Long): BranchMergeView {
    throw UnsupportedOperationException()
  }

  override fun getBranchMergeConflicts(
    projectId: Long,
    branchMergeId: Long,
    pageable: Pageable
  ): Page<BranchMergeConflictView> {
    throw UnsupportedOperationException()
  }

  override fun resolveConflict(projectId: Long, mergeId: Long, request: ResolveBranchMergeConflictRequest) {
    throw UnsupportedOperationException()
  }

  override fun applyMerge(projectId: Long, mergeId: Long) {
    throw UnsupportedOperationException()
  }
}
