package io.tolgee.service.branching

import io.tolgee.model.branching.Branch
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

class BranchServiceOssStub : BranchService {
  override fun getAllBranches(projectId: Long, page: Pageable, search: String?): Page<Branch> {
    return Page.empty()
  }
}
