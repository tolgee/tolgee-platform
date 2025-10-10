package io.tolgee.service.branching

import io.tolgee.dtos.request.branching.DryRunMergeBranchRequest
import io.tolgee.dtos.request.branching.MergeBranchRequest
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

  override fun mergeBranch(projectId: Long, branchId: Long, request: MergeBranchRequest) {
    throw UnsupportedOperationException()
  }

  override fun dryRunMergeBranch(projectId: Long, branchId: Long, request: DryRunMergeBranchRequest): BranchMerge {
    throw UnsupportedOperationException()
  }
}
