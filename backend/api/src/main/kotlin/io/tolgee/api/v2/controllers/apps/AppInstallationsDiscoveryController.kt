package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.apps.AppCredentialsRequest
import io.tolgee.hateoas.apps.AppSelfInstallationModel
import io.tolgee.hateoas.apps.AppSelfInstallationModelAssembler
import io.tolgee.security.authentication.AppAccessNeutral
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.apps.AppCredentialAuthenticator
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.service.apps.AppInstallService
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Lets an app discover its installations from its app-level credentials alone — the entry point of
 * the M2M flow, since minting an install-scoped token needs the install id this call returns.
 */
@RestController
@CrossOrigin(origins = ["*"])
@AppAccessNeutral
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/public/apps/installations"])
@Tag(name = "App Self Service")
class AppInstallationsDiscoveryController(
  private val appCredentialAuthenticator: AppCredentialAuthenticator,
  private val appInstallService: AppInstallService,
  private val appEnablementService: AppEnablementService,
  private val appSelfInstallationModelAssembler: AppSelfInstallationModelAssembler,
) {
  @PostMapping("/list")
  @RateLimited(30, isAuthentication = true)
  @Operation(
    summary = "List the calling app's installations",
    description =
      "Authenticates with the app-level client credentials and returns every installation of the " +
        "app on this server, with the projects each one is currently enabled for. The install ids " +
        "are what the token endpoint exchanges for install-scoped access tokens.",
  )
  fun list(
    @RequestBody @Valid body: AppCredentialsRequest,
  ): CollectionModel<AppSelfInstallationModel> {
    val app = appCredentialAuthenticator.authenticate(body.clientId, body.clientSecret)
    val models =
      appInstallService.findAllByRegisteredApp(app.id).map { install ->
        appSelfInstallationModelAssembler.toModel(
          install = install,
          native = false,
          enabledProjects = appEnablementService.listEnabledProjectsForInstall(install.id),
        )
      }
    return CollectionModel.of(models)
  }
}
