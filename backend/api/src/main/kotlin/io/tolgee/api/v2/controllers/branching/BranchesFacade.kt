package io.tolgee.api.v2.controllers.branching

import io.tolgee.api.mcp.McpFacade
import io.tolgee.model.branching.Branch
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BranchesFacade : McpFacade {
  fun getBranches(
    pageable: Pageable,
    search: String?,
  ): Page<Branch>

  fun createBranch(
    name: String,
    originBranchId: Long,
  ): Branch

  fun getActiveBranch(name: String): Branch

  fun deleteBranch(branchId: Long)
}
