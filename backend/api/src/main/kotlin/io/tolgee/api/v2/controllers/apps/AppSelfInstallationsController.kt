package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.exceptions.PermissionException
import io.tolgee.hateoas.apps.AppSelfInstallationModel
import io.tolgee.hateoas.apps.AppSelfInstallationModelAssembler
import io.tolgee.security.authentication.AllowAppOwnInstallAccess
import io.tolgee.security.authentication.AppAuthentication
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.service.apps.AppInstallService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Lets an app's backend discover what it is installed for, so it can do background work without a
 * user or an iframe.
 *
 * The install is taken from the token's own claims and never from the request, so the endpoint can
 * only ever describe the caller — see [AllowAppOwnInstallAccess], which is what admits an app token
 * here at all.
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
  @AllowAppOwnInstallAccess
  @Operation(
    summary = "List the calling app's own installations",
    description =
      "Returns the install the calling install-context token belongs to, together with the " +
        "projects the app is currently enabled for and the organization owning each of them. " +
        "Requires a token from the client-credentials grant — a user-context token (the one the " +
        "dashboard iframe gets) is refused, because it acts for a single user who need not be a " +
        "member of every project the install is enabled for. A collection is returned so that an " +
        "app holding several installs on one server stays representable.",
  )
  fun getSelfInstallations(): CollectionModel<AppSelfInstallationModel> {
    val install = requireInstallContext().appInstall
    val model =
      appSelfInstallationModelAssembler.toModel(
        install = install,
        native = appInstallService.isNative(install.id),
        enabledProjects = appEnablementService.listEnabledProjectsForInstall(install.id),
      )
    return CollectionModel.of(listOf(model))
  }

  private fun requireInstallContext(): AppAuthentication {
    if (!authenticationFacade.isAppAuth) throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
    val appAuthentication = authenticationFacade.appAuthentication
    if (!appAuthentication.isInstallContext) throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
    return appAuthentication
  }
}
