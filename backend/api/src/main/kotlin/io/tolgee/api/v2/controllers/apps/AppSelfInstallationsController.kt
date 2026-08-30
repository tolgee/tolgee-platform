package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.api.ISimpleProject
import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.apps.AppSelfInstallationModel
import io.tolgee.hateoas.apps.AppSelfInstallationModelAssembler
import io.tolgee.hateoas.project.SimpleProjectModel
import io.tolgee.hateoas.project.SimpleProjectModelAssembler
import io.tolgee.security.authentication.AllowAppLevelAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.service.apps.AppInstallService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/apps/self"])
@Tag(name = "App Self Service")
class AppSelfInstallationsController(
  private val authenticationFacade: AuthenticationFacade,
  private val appInstallService: AppInstallService,
  private val appEnablementService: AppEnablementService,
  private val appSelfInstallationModelAssembler: AppSelfInstallationModelAssembler,
  private val simpleProjectModelAssembler: SimpleProjectModelAssembler,
  private val pagedProjectAssembler: PagedResourcesAssembler<ISimpleProject>,
) {
  @GetMapping("/installations")
  @AllowAppLevelAccess
  @RateLimited(30, isAuthentication = true)
  @Operation(
    summary = "List the calling app's installations",
    description =
      "Returns every installation of the app on this server. The install ids are what the token " +
        "endpoint exchanges for install-scoped tokens; the projects each install may act on are " +
        "listed by the installation's `/projects` endpoint.",
  )
  fun listInstallations(): CollectionModel<AppSelfInstallationModel> {
    val appId = authenticationFacade.appAuthentication.appId
    val models = appInstallService.findAllByRegisteredApp(appId).map { appSelfInstallationModelAssembler.toModel(it) }
    return CollectionModel.of(models)
  }

  @PostMapping("/installations/{installId}/refresh")
  @AllowAppLevelAccess
  @RateLimited(10, isAuthentication = true)
  @RequestActivity(ActivityType.APP_UPDATE)
  @Operation(
    summary = "Re-fetch the manifest for one of the app's installations",
    description =
      "Re-reads the app's manifest and updates the stored snapshot. Never widens the install's " +
        "granted scopes: a manifest that requests more surfaces those as `pendingScopes` until the " +
        "organization's owner approves them; scopes the manifest no longer requests are dropped.",
  )
  fun refreshInstallation(
    @PathVariable installId: Long,
  ): AppSelfInstallationModel {
    val appId = authenticationFacade.appAuthentication.appId
    return appSelfInstallationModelAssembler.toModel(appInstallService.refreshForApp(appId, installId))
  }

  @GetMapping("/installations/{installId}/projects")
  @AllowAppLevelAccess
  @RateLimited(30, isAuthentication = true)
  @Operation(
    summary = "List the projects an installation is enabled for",
    description =
      "Paginated list of the projects the given installation of the calling app may currently act " +
        "on. The list changes whenever a project owner enables or disables the app.",
  )
  fun listEnabledProjects(
    @PathVariable installId: Long,
    @ParameterObject pageable: Pageable,
  ): PagedModel<SimpleProjectModel> {
    val appId = authenticationFacade.appAuthentication.appId
    appInstallService.findOwnInstall(appId, installId)
      ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    val projects: Page<ISimpleProject> =
      appEnablementService.getEnabledProjectsForInstall(installId, pageable).map { it }
    return pagedProjectAssembler.toModel(projects, simpleProjectModelAssembler)
  }
}
