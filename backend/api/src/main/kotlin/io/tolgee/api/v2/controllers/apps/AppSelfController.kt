package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.request.RegisterAppRequest
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.hateoas.organization.apps.AppInstallModel
import io.tolgee.hateoas.organization.apps.AppInstallModelAssembler
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.apps.AppInstallService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoints a plugin install can call about itself. The auth filter accepts
 * `X-API-Key: tgapps_<clientSecret>` (or HTTP Basic with `clientId:clientSecret`)
 * and resolves it to the calling [AppInstall]; this controller pulls the install
 * out of the security context, so callers can't reach other installs.
 */
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/apps/self"])
@Tag(name = "Self-service plugin API")
class AppSelfController(
  private val authenticationFacade: AuthenticationFacade,
  private val appInstallService: AppInstallService,
  private val appInstallModelAssembler: AppInstallModelAssembler,
) {
  @PatchMapping("/manifest-url")
  @Operation(
    summary = "Update own manifest URL",
    description =
      "Repoints the install at a new manifest URL and re-fetches the manifest. " +
        "The new manifest must declare the same `id` as the original — a plugin can " +
        "swap its dev tunnel URL between restarts but can't masquerade as a different " +
        "plugin. Authenticated by the install's `clientSecret`.",
  )
  fun updateManifestUrl(
    @RequestBody body: RegisterAppRequest,
  ): AppInstallModel {
    val install = currentInstall()
    val updated =
      appInstallService.updateManifestUrl(
        organizationId = install.organization.id,
        installId = install.id,
        manifestUrl = body.manifestUrl,
        // Plugin-authenticated call: must never widen its own granted scopes.
        allowScopeWidening = false,
      )
    return appInstallModelAssembler.toModel(updated)
  }

  private fun currentInstall() =
    run {
      val auth =
        runCatching { authenticationFacade.appAuthentication }
          .getOrNull()
          ?: throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
      if (!auth.isInstallContext) {
        throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
      }
      auth.appInstall
    }
}
