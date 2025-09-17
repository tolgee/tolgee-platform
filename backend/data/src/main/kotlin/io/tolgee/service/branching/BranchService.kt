package io.tolgee.service.branching

import io.tolgee.model.branching.Branch
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BranchService {
  fun getAllBranches(projectId: Long, page: Pageable, search: String? = null): Page<Branch>
}
