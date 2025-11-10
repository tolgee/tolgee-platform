/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.ActivityService
import io.tolgee.exceptions.NotFoundException
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
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/activity", "/v2/projects/activity"])
@Tag(name = "Projects")
class ProjectActivityController(
  private val activityService: ActivityService,
  private val projectHolder: ProjectHolder,
  private val activityPagedResourcesAssembler: PagedResourcesAssembler<ProjectActivityView>,
  private val modificationResourcesAssembler: PagedResourcesAssembler<ModifiedEntityView>,
  private val projectActivityModelAssembler: ProjectActivityModelAssembler,
  private val modifiedEntityModelAssembler: ModifiedEntityModelAssembler,
) {
  @Operation(summary = "Get project activity")
  @GetMapping("")
  @RequiresProjectPermissions([Scope.ACTIVITY_VIEW])
  @AllowApiAccess
  fun getActivity(
    @ParameterObject pageable: Pageable,
  ): PagedModel<ProjectActivityModel> {
    val views = activityService.findProjectActivity(projectId = projectHolder.project.id, pageable)
    return activityPagedResourcesAssembler.toModel(views, projectActivityModelAssembler)
  }

  @Operation(summary = "Get one revision data")
  @GetMapping("/revisions/{revisionId}")
  @RequiresProjectPermissions([Scope.ACTIVITY_VIEW])
  @AllowApiAccess
  fun getSingleRevision(
    @PathVariable revisionId: Long,
  ): ProjectActivityModel {
    val views =
      activityService.findProjectActivity(projectId = projectHolder.project.id, revisionId)
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
}
