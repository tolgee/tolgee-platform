package io.tolgee.ee.api.v2.controllers.branching

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.api.v2.hateoas.assemblers.BranchModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchModel
import io.tolgee.ee.api.v2.hateoas.model.branching.CreateBranchModel
import io.tolgee.ee.service.branching.BranchServiceImpl
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/branches",
  ],
)
@OpenApiOrderExtension(8)
@Tag(name = "Branches", description = "Branching operations")
class BranchController(
  private val branchService: BranchServiceImpl,
  private val projectHolder: ProjectHolder,
  private val branchModelAssembler: BranchModelAssembler,
  private val pagedResourceAssembler: PagedResourcesAssembler<Branch>,
) {
  @GetMapping(value = [""])
  @Operation(summary = "Get all branches")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @OpenApiOrderExtension(1)
  fun all(
    @ParameterObject
    pageable: Pageable,
    @RequestParam("search", required = false)
    search: String?,
  ): PagedModel<BranchModel> {
    val branches = branchService.getAllBranches(projectHolder.project.id, pageable, search)
    return pagedResourceAssembler.toModel(branches, branchModelAssembler)
  }

  @PostMapping(value = [""])
  @Operation(summary = "Create branch")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @OpenApiOrderExtension(2)
  fun create(@RequestBody branch: CreateBranchModel): BranchModel {
    val branch = branchService.createBranch(projectHolder.project.id, branch)
    return branchModelAssembler.toModel(branch)
  }

  @DeleteMapping(value = ["/{branchId}"])
  @Operation(summary = "Delete branch")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @OpenApiOrderExtension(3)
  fun delete(@PathVariable branchId: Long) {
    branchService.deleteBranch(projectHolder.project.id, branchId)
  }
}
