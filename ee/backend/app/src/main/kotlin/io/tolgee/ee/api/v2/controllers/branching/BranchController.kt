package io.tolgee.ee.api.v2.controllers.branching

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.queryResults.branching.BranchMergeChangeView
import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.dtos.request.branching.ApplyBranchMergeRequest
import io.tolgee.dtos.request.branching.DryRunMergeBranchRequest
import io.tolgee.dtos.request.branching.ResolveAllBranchMergeConflictsRequest
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.ee.api.v2.hateoas.assemblers.branching.BranchMergeChangeModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.branching.BranchMergeConflictModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.branching.BranchMergeModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.branching.BranchMergeRefModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.branching.BranchModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeChangeModel
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeConflictModel
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeModel
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeRefModel
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchModel
import io.tolgee.ee.api.v2.hateoas.model.branching.CreateBranchModel
import io.tolgee.ee.api.v2.hateoas.model.branching.RenameBranchModel
import io.tolgee.ee.api.v2.hateoas.model.branching.SetBranchProtectedModel
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
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
import org.springframework.web.bind.annotation.PutMapping
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
    "/v2/projects/branches",
  ],
)
@OpenApiOrderExtension(9)
@Tag(name = "Branches", description = "Branching operations")
class BranchController(
  private val branchService: BranchService,
  private val projectHolder: ProjectHolder,
  private val branchModelAssembler: BranchModelAssembler,
  private val pagedBranchResourceAssembler: PagedResourcesAssembler<Branch>,
  private val branchMergeModelAssembler: BranchMergeModelAssembler,
  private val pagedBranchMergeResourceAssembler: PagedResourcesAssembler<BranchMergeView>,
  private val branchMergeConflictModelAssembler: BranchMergeConflictModelAssembler,
  private val pagedBranchMergeConflictResourceAssembler: PagedResourcesAssembler<BranchMergeConflictView>,
  private val branchMergeChangeModelAssembler: BranchMergeChangeModelAssembler,
  private val pagedBranchMergeChangeResourceAssembler: PagedResourcesAssembler<BranchMergeChangeView>,
  private val branchMergeRefModelAssembler: BranchMergeRefModelAssembler,
  private val authenticationFacade: AuthenticationFacade,
) {
  @PostMapping(value = [""])
  @Operation(summary = "Create branch")
  @RequiresProjectPermissions([Scope.BRANCH_MANAGEMENT])
  @AllowApiAccess
  @OpenApiOrderExtension(2)
  fun create(
    @RequestBody branch: CreateBranchModel,
  ): BranchModel {
    val branch =
      branchService.createBranch(
        projectHolder.project.id,
        branch.name,
        branch.originBranchId,
        authenticationFacade.authenticatedUserEntity,
      )
    return branchModelAssembler.toModel(branch)
  }

  @GetMapping(value = [""])
  @Operation(summary = "Get all branches")
  @AllowApiAccess
  @UseDefaultPermissions
  @OpenApiOrderExtension(1)
  fun all(
    @ParameterObject
    pageable: Pageable,
    @RequestParam("search", required = false)
    search: String?,
    @RequestParam("activeOnly", required = false)
    activeOnly: Boolean?,
  ): PagedModel<BranchModel> {
    val branches = branchService.getBranches(projectHolder.project.id, pageable, search, activeOnly)
    return pagedBranchResourceAssembler.toModel(branches, branchModelAssembler)
  }

  @DeleteMapping(value = ["/{branchId}"])
  @Operation(summary = "Delete branch")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.BRANCH_MANAGEMENT])
  @OpenApiOrderExtension(3)
  fun delete(
    @PathVariable branchId: Long,
  ) {
    branchService.deleteBranch(projectHolder.project.id, branchId)
  }

  @PostMapping(value = ["/{branchId}"])
  @Operation(summary = "Rename branch")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.BRANCH_MANAGEMENT])
  @OpenApiOrderExtension(3)
  fun rename(
    @PathVariable branchId: Long,
    @RequestBody rename: RenameBranchModel,
  ): BranchModel {
    val branch = branchService.renameBranch(projectHolder.project.id, branchId, rename.name)
    return branchModelAssembler.toModel(branch)
  }

  @PostMapping(value = ["/{branchId}/protected"])
  @Operation(summary = "Set branch protected flag")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.BRANCH_MANAGEMENT])
  @OpenApiOrderExtension(3)
  fun setProtected(
    @PathVariable branchId: Long,
    @RequestBody request: SetBranchProtectedModel,
  ): BranchModel {
    val branch = branchService.setProtected(projectHolder.project.id, branchId, request.isProtected)
    return branchModelAssembler.toModel(branch)
  }

  @GetMapping(value = ["/merge"])
  @Operation(summary = "Get branch merges")
  @AllowApiAccess
  @UseDefaultPermissions
  @OpenApiOrderExtension(4)
  fun getBranchMerges(
    @ParameterObject
    pageable: Pageable,
  ): PagedModel<BranchMergeModel> {
    val merges = branchService.getBranchMerges(projectHolder.project.id, pageable)
    return pagedBranchMergeResourceAssembler.toModel(merges, branchMergeModelAssembler)
  }

  @PostMapping(value = ["/merge/preview"])
  @Operation(summary = "Creates a merge, dry-runs source branch to target branch and return preview")
  @AllowApiAccess
  @UseDefaultPermissions
  @OpenApiOrderExtension(5)
  fun dryRunMerge(
    @RequestBody request: DryRunMergeBranchRequest,
  ): BranchMergeRefModel {
    val merge = branchService.dryRunMerge(projectHolder.project.id, request)
    return branchMergeRefModelAssembler.toModel(merge)
  }

  @GetMapping(value = ["/merge/{mergeId}/preview"])
  @Operation(summary = "Get branch merge session preview")
  @AllowApiAccess
  @UseDefaultPermissions
  @OpenApiOrderExtension(6)
  fun getBranchMergeSessionPreview(
    @PathVariable mergeId: Long,
  ): BranchMergeModel {
    val merge = branchService.getBranchMergeView(projectHolder.project.id, mergeId)
    return branchMergeModelAssembler.toModel(merge)
  }

  @PostMapping(value = ["/merge/{mergeId}/refresh"])
  @Operation(summary = "Refresh branch merge session preview")
  @AllowApiAccess
  @UseDefaultPermissions
  @OpenApiOrderExtension(11)
  fun refreshBranchMerge(
    @PathVariable mergeId: Long,
  ): BranchMergeModel {
    val merge = branchService.refreshMerge(projectHolder.project.id, mergeId)
    return branchMergeModelAssembler.toModel(merge)
  }

  @GetMapping(value = ["/merge/{mergeId}/conflicts"])
  @Operation(summary = "Get branch merge session conflicts")
  @AllowApiAccess
  @UseDefaultPermissions
  @OpenApiOrderExtension(7)
  fun getBranchMergeSessionConflicts(
    @ParameterObject
    pageable: Pageable,
    @PathVariable mergeId: Long,
  ): PagedModel<BranchMergeConflictModel> {
    val conflicts = branchService.getBranchMergeConflicts(projectHolder.project.id, mergeId, pageable)
    return pagedBranchMergeConflictResourceAssembler.toModel(conflicts, branchMergeConflictModelAssembler)
  }

  @GetMapping(value = ["/merge/{mergeId}/changes"])
  @Operation(summary = "Get branch merge session changes")
  @AllowApiAccess
  @UseDefaultPermissions
  @OpenApiOrderExtension(7)
  fun getBranchMergeSessionChanges(
    @ParameterObject pageable: Pageable,
    @PathVariable mergeId: Long,
    @RequestParam(required = false) type: BranchKeyMergeChangeType?,
  ): PagedModel<BranchMergeChangeModel> {
    val changes = branchService.getBranchMergeChanges(projectHolder.project.id, mergeId, type, pageable)
    return pagedBranchMergeChangeResourceAssembler.toModel(changes, branchMergeChangeModelAssembler)
  }

  @PutMapping(value = ["/merge/{mergeId}/resolve"])
  @Operation(summary = "Resolve branch merge session conflicts")
  @AllowApiAccess
  @UseDefaultPermissions
  @OpenApiOrderExtension(8)
  fun resolveConflict(
    @PathVariable mergeId: Long,
    @RequestBody request: ResolveBranchMergeConflictRequest,
  ) {
    branchService.resolveConflict(projectHolder.project.id, mergeId, request)
  }

  @PutMapping(value = ["/merge/{mergeId}/resolve-all"])
  @Operation(summary = "Resolve all branch merge session conflicts")
  @AllowApiAccess
  @UseDefaultPermissions
  @OpenApiOrderExtension(8)
  fun resolveAllConflicts(
    @PathVariable mergeId: Long,
    @RequestBody request: ResolveAllBranchMergeConflictsRequest,
  ) {
    branchService.resolveAllConflicts(projectHolder.project.id, mergeId, request)
  }

  @DeleteMapping(value = ["/merge/{mergeId}"])
  @Operation(summary = "Delete branch merge session")
  @AllowApiAccess
  @UseDefaultPermissions
  @OpenApiOrderExtension(9)
  fun deleteBranchMerge(
    @PathVariable mergeId: Long,
  ) {
    branchService.deleteMerge(projectHolder.project.id, mergeId)
  }

  @PostMapping(value = ["/merge/{mergeId}/apply"])
  @Operation(summary = "Merge source branch to target branch")
  @AllowApiAccess
  @UseDefaultPermissions
  @OpenApiOrderExtension(10)
  fun merge(
    @PathVariable mergeId: Long,
    @RequestBody(required = false) request: ApplyBranchMergeRequest?,
  ) {
    branchService.applyMerge(projectHolder.project.id, mergeId, request?.deleteBranch ?: true)
  }
}
