/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.request.project.CommunityPageRequest
import io.tolgee.dtos.request.project.SetProjectPublicRequest
import io.tolgee.hateoas.project.ProjectModel
import io.tolgee.hateoas.project.ProjectModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.project.ProjectService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress(names = ["MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection"])
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects"])
@Tag(
  name = "Project Publishing",
  description =
    "Opens a project to community translation or closes it, and sets what its community page shows " +
      "(organization owner or server admin)",
)
class ProjectsPublishingController(
  private val projectService: ProjectService,
  private val projectHolder: ProjectHolder,
  private val organizationRoleService: OrganizationRoleService,
  private val projectModelAssembler: ProjectModelAssembler,
) {
  @PutMapping(value = ["/{projectId:[0-9]+}/publishing"])
  @Operation(
    summary = "Set project publishing state",
    description =
      "Marks the project as public or private. Opening a private project also turns suggestions on " +
        "(`suggestionsMode` becomes `ENABLED`); closing it, or opening an already public project, leaves " +
        "`suggestionsMode` as it is. Only the organization owner or a server admin can change this.",
  )
  @RequestActivity(ActivityType.EDIT_PROJECT)
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @RequiresSuperAuthentication
  fun setProjectPublic(
    @RequestBody @Valid
    dto: SetProjectPublicRequest,
  ): ProjectModel {
    organizationRoleService.checkUserIsOwnerOrServerAdmin(projectHolder.project.organizationOwnerId)
    val project = projectService.setPublic(projectHolder.project.id, dto.public)
    return projectModelAssembler.toModel(projectService.getView(project.id))
  }

  @PutMapping(value = ["/{projectId:[0-9]+}/community-page"])
  @Operation(
    summary = "Set community page settings",
    description =
      "Sets the texts and flags shown on the project's community page. All fields are replaced: a missing or empty " +
        "text clears it. The settings stay when the project is closed to the community. " +
        "Only the organization owner or a server admin can change them.",
  )
  @RequestActivity(ActivityType.EDIT_PROJECT)
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @RequiresSuperAuthentication
  fun setCommunityPage(
    @RequestBody @Valid
    dto: CommunityPageRequest,
  ): ProjectModel {
    organizationRoleService.checkUserIsOwnerOrServerAdmin(projectHolder.project.organizationOwnerId)
    val project = projectService.setCommunityPage(projectHolder.project.id, dto)
    return projectModelAssembler.toModel(projectService.getView(project.id))
  }
}
