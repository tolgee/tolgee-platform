/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
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
  description = "Marks a project as public or private (organization owner or server admin)",
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
      "Marks the project as public or private. " +
        "Only the organization owner or a server admin can change this.",
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
}
