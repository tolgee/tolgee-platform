package io.tolgee.service.branching

import io.tolgee.model.branching.Branch
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class BranchServiceOssStub : BranchService {
  override fun getAllBranches(projectId: Long, page: Pageable, search: String?): Page<Branch> {
    return Page.empty()
  }
  override fun createBranch(projectId: Long, name: String, originBranchId: Long): Branch {
    throw UnsupportedOperationException()
  }

  override fun deleteBranch(projectId: Long, branchId: Long) {
    throw UnsupportedOperationException()
  }
}
