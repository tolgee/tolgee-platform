/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.ActivityService
import io.tolgee.api.v2.hateoas.activity.ProjectActivityModel
import io.tolgee.api.v2.hateoas.activity.ProjectActivityModelAssembler
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.MediaTypes
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId}/activity", "/v2/projects/activity"])
@Tag(name = "Projects")
class ProjectActivityController(
  private val activityService: ActivityService,
  private val projectHolder: ProjectHolder,
  private val pagedResourcesAssembler: PagedResourcesAssembler<ProjectActivityView>,
  private val projectActivityModelAssembler: ProjectActivityModelAssembler
) {
  @Operation(summary = "Returns project history")
  @AccessWithAnyProjectPermission
  @AccessWithApiKey(scopes = [ApiScope.ACTIVITY_VIEW])
  @GetMapping("", produces = [MediaTypes.HAL_JSON_VALUE])
  fun getActivity(
    @ParameterObject pageable: Pageable,
  ): PagedModel<ProjectActivityModel> {
    val views = activityService.getProjectActivity(projectId = projectHolder.project.id, pageable)
    return pagedResourcesAssembler.toModel(views, projectActivityModelAssembler)
  }
}
