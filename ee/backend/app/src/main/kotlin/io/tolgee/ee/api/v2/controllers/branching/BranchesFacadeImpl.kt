package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.api.mcp.checkValid
import io.tolgee.api.v2.controllers.branching.BranchesFacade
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.model.branching.CreateBranchModel
import io.tolgee.model.branching.Branch
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.branching.BranchService
import io.tolgee.service.project.ProjectFeatureGuard
import jakarta.validation.Validator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class BranchesFacadeImpl(
  private val branchService: BranchService,
  private val projectHolder: ProjectHolder,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val authenticationFacade: AuthenticationFacade,
  private val validator: Validator,
) : BranchesFacade {
  override fun getBranches(
    pageable: Pageable,
    search: String?,
  ): Page<Branch> {
    projectFeatureGuard.checkEnabled(Feature.BRANCHING)
    return branchService.getBranches(projectHolder.project.id, pageable, search)
  }

  override fun createBranch(
    name: String,
    originBranchId: Long,
  ): Branch {
    validator.checkValid(CreateBranchModel(name, originBranchId))
    projectFeatureGuard.checkEnabled(Feature.BRANCHING)
    return branchService.createBranch(
      projectHolder.project.id,
      name,
      originBranchId,
      authenticationFacade.authenticatedUserEntity,
    )
  }

  override fun getActiveBranch(name: String): Branch {
    projectFeatureGuard.checkEnabled(Feature.BRANCHING)
    return branchService.getActiveBranch(projectHolder.project.id, name)
  }

  override fun deleteBranch(branchId: Long) {
    projectFeatureGuard.checkEnabled(Feature.BRANCHING)
    branchService.deleteBranch(projectHolder.project.id, branchId)
  }
}
