package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.project.apps.ProjectAppModel
import io.tolgee.hateoas.project.apps.ProjectAppModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.apps.AppEnablementService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/apps"])
@Tag(name = "Project Apps")
class ProjectAppsController(
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val appEnablementService: AppEnablementService,
  private val projectAppModelAssembler: ProjectAppModelAssembler,
) {
  @GetMapping
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @Operation(
    summary = "List apps for project",
    description =
      "Returns all apps registered in the project's organization, each annotated with whether it is " +
        "enabled for this project. Requires project.edit: it discloses the organization's whole app " +
        "inventory, including apps not enabled for this project.",
  )
  fun list(
    @PathVariable projectId: Long,
  ): CollectionModel<ProjectAppModel> {
    val project = projectHolder.projectEntity
    val results = appEnablementService.listAppsForProject(project)
    val models = results.map { projectAppModelAssembler.toModel(it.install, it.enabled) }
    return CollectionModel.of(models)
  }

  @GetMapping("/enabled")
  @UseDefaultPermissions
  @Operation(
    summary = "List apps enabled for project",
    description =
      "Returns only the apps enabled for this project, which every project member needs to render " +
        "their dashboard pages. Discloses nothing about the organization's other apps.",
  )
  fun listEnabled(
    @PathVariable projectId: Long,
  ): CollectionModel<ProjectAppModel> {
    val installs = appEnablementService.listEnabledInstallsForProject(projectHolder.project.id)
    val models = installs.map { projectAppModelAssembler.toModel(it, enabled = true) }
    return CollectionModel.of(models)
  }

  @PutMapping("/{installId}")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @Operation(
    summary = "Enable app for project",
    description = "Enables the given app install for this project. Idempotent.",
  )
  fun enable(
    @PathVariable projectId: Long,
    @PathVariable installId: Long,
  ): ProjectAppModel {
    val install =
      appEnablementService.enable(
        project = projectHolder.projectEntity,
        installId = installId,
      )
    return projectAppModelAssembler.toModel(install, enabled = true)
  }

  @DeleteMapping("/{installId}")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @Operation(
    summary = "Disable app for project",
    description = "Disables the given app for this project. Idempotent — no-op if it wasn't enabled.",
  )
  fun disable(
    @PathVariable projectId: Long,
    @PathVariable installId: Long,
  ) {
    appEnablementService.disable(projectHolder.project.id, installId)
  }
}
