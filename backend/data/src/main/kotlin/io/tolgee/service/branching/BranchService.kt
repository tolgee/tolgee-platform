package io.tolgee.service.branching

import io.tolgee.model.branching.Branch
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BranchService {
  fun getAllBranches(projectId: Long, page: Pageable, search: String? = null): Page<Branch>
  fun getBranch(projectId: Long, branchId: Long): Branch
  fun createBranch(projectId: Long, name: String, originBranchId: Long): Branch
  fun deleteBranch(projectId: Long, branchId: Long)
}
