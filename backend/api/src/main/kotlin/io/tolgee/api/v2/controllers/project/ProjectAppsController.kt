package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.ProjectAppView
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.project.apps.AppTokenModel
import io.tolgee.hateoas.project.apps.ProjectAppModel
import io.tolgee.hateoas.project.apps.ProjectAppModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.DenyAppAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.apps.AppEnablementService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/apps"])
@Tag(name = "Project Apps")
@DenyAppAccess
class ProjectAppsController(
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val appEnablementService: AppEnablementService,
  private val appTokenService: AppTokenService,
  private val projectAppModelAssembler: ProjectAppModelAssembler,
  private val pagedProjectAppsAssembler: PagedResourcesAssembler<ProjectAppView>,
) {
  @GetMapping
  @RequiresProjectPermissions([Scope.APPS_MANAGE])
  @Operation(
    summary = "List all apps for project (management view)",
    description =
      "Returns every app the project's organization has installed, each annotated with whether it is " +
        "enabled for this project. Requires apps.manage: it discloses the organization's whole app " +
        "inventory, including apps not enabled for this project. The default project view uses the " +
        "`/enabled` listing instead.",
  )
  fun list(
    @PathVariable projectId: Long,
    @ParameterObject pageable: Pageable,
  ): PagedModel<ProjectAppModel> {
    val page = appEnablementService.listAppsForProject(projectHolder.projectEntity, pageable)
    return pagedProjectAppsAssembler.toModel(page, projectAppModelAssembler)
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
  @RequiresProjectPermissions([Scope.APPS_MANAGE])
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
  @RequiresProjectPermissions([Scope.APPS_MANAGE])
  @Operation(
    summary = "Disable app for project",
    description = "Disables the given app for this project. Idempotent - no-op if it wasn't enabled.",
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
      "Issues a short-lived JWT bound to (install, project, current user) that the dashboard iframe " +
        "uses to call Tolgee's REST API on behalf of the user. Returns 404 if the install is not " +
        "enabled for this project.",
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
        isReadOnly = authenticationFacade.isReadOnly,
      )
    return AppTokenModel(token = token)
  }
}
