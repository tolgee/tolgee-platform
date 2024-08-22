/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.ActivityService
import io.tolgee.activity.groups.ActivityGroupService
import io.tolgee.dtos.queryResults.ActivityGroupView
import io.tolgee.dtos.request.ActivityGroupFilters
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.activity.ActivityGroupModel
import io.tolgee.hateoas.activity.ActivityGroupModelAssembler
import io.tolgee.hateoas.activity.ModifiedEntityModel
import io.tolgee.hateoas.activity.ModifiedEntityModelAssembler
import io.tolgee.hateoas.activity.ProjectActivityModel
import io.tolgee.hateoas.activity.ProjectActivityModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.activity.ModifiedEntityView
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.MediaTypes
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId}/activity", "/v2/projects/activity"])
@Tag(name = "Projects")
class ProjectActivityController(
  private val activityService: ActivityService,
  private val projectHolder: ProjectHolder,
  private val activityPagedResourcesAssembler: PagedResourcesAssembler<ProjectActivityView>,
  private val modificationResourcesAssembler: PagedResourcesAssembler<ModifiedEntityView>,
  private val projectActivityModelAssembler: ProjectActivityModelAssembler,
  private val modifiedEntityModelAssembler: ModifiedEntityModelAssembler,
  private val activityGroupService: ActivityGroupService,
  private val groupPagedResourcesAssembler: PagedResourcesAssembler<ActivityGroupView>,
  private val groupModelAssembler: ActivityGroupModelAssembler,
  private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
) {
  @Operation(summary = "Get project activity")
  @GetMapping("", produces = [MediaTypes.HAL_JSON_VALUE])
  @RequiresProjectPermissions([ Scope.ACTIVITY_VIEW ])
  @AllowApiAccess
  fun getActivity(
    @ParameterObject pageable: Pageable,
  ): PagedModel<ProjectActivityModel> {
    val views = activityService.getProjectActivity(projectId = projectHolder.project.id, pageable)
    return activityPagedResourcesAssembler.toModel(views, projectActivityModelAssembler)
  }

  @Operation(summary = "Get one revision data")
  @GetMapping("/revisions/{revisionId}", produces = [MediaTypes.HAL_JSON_VALUE])
  @RequiresProjectPermissions([Scope.ACTIVITY_VIEW])
  @AllowApiAccess
  fun getSingleRevision(
    @PathVariable revisionId: Long,
  ): ProjectActivityModel {
    val views =
      activityService.getProjectActivity(projectId = projectHolder.project.id, revisionId)
        ?: throw NotFoundException()
    return projectActivityModelAssembler.toModel(views)
  }

  @Operation(summary = "Get modified entities in revision")
  @GetMapping("/revisions/{revisionId}/modified-entities")
  @RequiresProjectPermissions([Scope.ACTIVITY_VIEW])
  fun getModifiedEntitiesByRevision(
    @ParameterObject pageable: Pageable,
    @PathVariable revisionId: Long,
    @Parameter(
      description = "Filters results by specific entity class",
      examples = [ExampleObject(value = "Key"), ExampleObject(value = "Translation")],
    )
    @RequestParam(required = false)
    filterEntityClass: List<String>?,
  ): PagedModel<ModifiedEntityModel> {
    val page =
      activityService.getRevisionModifications(
        projectId = projectHolder.project.id,
        revisionId,
        pageable,
        filterEntityClass,
      )
    return modificationResourcesAssembler.toModel(page, modifiedEntityModelAssembler)
  }

  @Operation(
    summary = "Get project activity groups",
    description = "This endpoints returns the activity grouped by time windows so it's easier to read on the frontend.",
  )
  @GetMapping("/groups", produces = [MediaTypes.HAL_JSON_VALUE])
  @RequiresProjectPermissions([Scope.ACTIVITY_VIEW])
  @AllowApiAccess
  fun getActivityGroups(
    @ParameterObject pageable: Pageable,
    @ParameterObject activityGroupFilters: ActivityGroupFilters,
  ): PagedModel<ActivityGroupModel> {
    val views =
      activityGroupService.getProjectActivityGroups(
        projectId = projectHolder.project.id,
        pageable,
        activityGroupFilters,
      )
    return groupPagedResourcesAssembler.toModel(views, groupModelAssembler)
  }
}
