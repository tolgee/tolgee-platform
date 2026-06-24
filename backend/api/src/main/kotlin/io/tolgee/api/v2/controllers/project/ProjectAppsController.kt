package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.project.apps.AppTokenModel
import io.tolgee.hateoas.project.apps.ProjectAppModel
import io.tolgee.hateoas.project.apps.ProjectAppModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.apps.AppEnablementService
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/apps"])
@Tag(name = "Project Apps")
class ProjectAppsController(
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val appEnablementService: AppEnablementService,
  private val appTokenService: AppTokenService,
  private val projectAppModelAssembler: ProjectAppModelAssembler,
) {
  @GetMapping
  @UseDefaultPermissions
  @Operation(
    summary = "List apps for project",
    description =
      "Returns all apps registered in the project's organization, " +
        "each annotated with whether it is enabled for this project.",
  )
  fun list(
    @PathVariable projectId: Long,
  ): CollectionModel<ProjectAppModel> {
    val project = projectHolder.projectEntity
    val results = appEnablementService.listAppsForProject(project)
    val models = results.map { (install, enabled) -> projectAppModelAssembler.toModel(install, enabled) }
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
        author = authenticationFacade.authenticatedUserEntity,
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

  @PostMapping("/{installId}/token")
  @UseDefaultPermissions
  @Operation(
    summary = "Mint a user-context app token",
    description =
      "Issues a short-lived JWT bound to (install, project, current user) that the iframe can use to call " +
        "Tolgee's REST API on behalf of the user. Returns 404 if the install is not enabled for this project.",
  )
  fun mintToken(
    @PathVariable projectId: Long,
    @PathVariable installId: Long,
  ): AppTokenModel {
    if (!appEnablementService.isEnabledForProject(projectId, installId)) {
      throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    }
    val token =
      appTokenService.mintUserContextToken(
        installId = installId,
        userId = authenticationFacade.authenticatedUser.id,
        projectId = projectId,
      )
    return AppTokenModel(token = token)
  }
}
