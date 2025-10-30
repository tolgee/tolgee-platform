package io.tolgee.ee.api.v2.controllers.branching

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.ee.api.v2.hateoas.assemblers.branching.BranchModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchModel
import io.tolgee.ee.api.v2.hateoas.model.branching.CreateBranchModel
import io.tolgee.dtos.request.branching.DryRunMergeBranchRequest
import io.tolgee.ee.api.v2.hateoas.assemblers.branching.BranchMergeConflictModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.branching.BranchMergeModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeConflictModel
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeModel
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeRefModel
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.branching.BranchService
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
  private val branchService: BranchService,
  private val projectHolder: ProjectHolder,
  private val branchModelAssembler: BranchModelAssembler,
  private val pagedBranchResourceAssembler: PagedResourcesAssembler<Branch>,
  private val branchMergeModelAssembler: BranchMergeModelAssembler,
  private val branchMergeConflictModelAssembler: BranchMergeConflictModelAssembler,
  private val pagedBranchMergeConflictResourceAssembler: PagedResourcesAssembler<BranchMergeConflictView>,

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
    return pagedBranchResourceAssembler.toModel(branches, branchModelAssembler)
  }

  @PostMapping(value = [""])
  @Operation(summary = "Create branch")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @OpenApiOrderExtension(2)
  fun create(@RequestBody branch: CreateBranchModel): BranchModel {
    val branch = branchService.createBranch(
      projectHolder.project.id,
      branch.name,
      branch.originBranchId
    )
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

//  @PostMapping(value = ["/{branchId}/merge"])
//  @Operation(summary = "Merge source branch to target branch")
//  @AllowApiAccess
//  @RequiresProjectPermissions([Scope.KEYS_EDIT])
//  @OpenApiOrderExtension(4)
//  fun merge(
//    @PathVariable branchId: Long,
//    @RequestBody request: MergeBranchRequest,
//  ) {
//    branchService.mergeBranch(projectHolder.project.id, branchId, request)
//  }

  @PostMapping(value = ["/{branchId}/merge/preview"])
  @Operation(summary = "Dry-run merge source branch to target branch and return preview")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @OpenApiOrderExtension(5)
  fun dryRunMerge(
    @PathVariable branchId: Long,
    @RequestBody request: DryRunMergeBranchRequest,
  ): BranchMergeRefModel {
    val merge = branchService.dryRunMergeBranch(projectHolder.project.id, branchId, request)
    return BranchMergeRefModel(merge.id)
  }

  @GetMapping(value = ["/{branchId}/merge/preview/{mergeId}"])
  @Operation(summary = "Get branch merge session preview")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @OpenApiOrderExtension(6)
  fun getBranchMergeSessionPreview(
    @PathVariable branchId: Long,
    @PathVariable mergeId: Long,
  ): BranchMergeModel {
    val merge = branchService.getBranchMerge(projectHolder.project.id, branchId, mergeId)
    return branchMergeModelAssembler.toModel(merge)
  }

  @GetMapping(value = ["/{branchId}/merge/preview/{mergeId}/conflicts"])
  @Operation(summary = "Get branch merge session conflicts")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @OpenApiOrderExtension(7)
  fun getBranchMergeSessionConflicts(
    @ParameterObject
    pageable: Pageable,
    @PathVariable branchId: Long,
    @PathVariable mergeId: Long,
  ): PagedModel<BranchMergeConflictModel> {
    val conflicts = branchService.getBranchMergeConflicts(projectHolder.project.id, branchId, mergeId, pageable)
    return pagedBranchMergeConflictResourceAssembler.toModel(conflicts, branchMergeConflictModelAssembler)
  }
}
