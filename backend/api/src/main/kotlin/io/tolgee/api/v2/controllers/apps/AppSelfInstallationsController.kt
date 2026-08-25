package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.apps.AppSelfInstallationModel
import io.tolgee.hateoas.apps.AppSelfInstallationModelAssembler
import io.tolgee.security.authentication.AllowAppLevelAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.service.apps.AppInstallService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Lets an app's backend discover all of its installations so it can do background work without a
 * user or an iframe. Authenticated by an app-level token (client credentials without an install id),
 * so the raw client secret only ever travels to the token endpoint.
 */
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
) {
  @GetMapping("/installations")
  @AllowAppLevelAccess
  @RateLimited(30, isAuthentication = true)
  @Operation(
    summary = "List the calling app's installations",
    description =
      "Returns every installation of the app on this server, with the projects each one is currently " +
        "enabled for. The install ids are what the token endpoint exchanges for install-scoped tokens.",
  )
  fun listInstallations(): CollectionModel<AppSelfInstallationModel> {
    val appId = authenticationFacade.appAuthentication.appId
    val models =
      appInstallService.findAllByRegisteredApp(appId).map { install ->
        appSelfInstallationModelAssembler.toModel(
          install = install,
          native = false,
          enabledProjects = appEnablementService.listEnabledProjectsForInstall(install.id),
        )
      }
    return CollectionModel.of(models)
  }
}
