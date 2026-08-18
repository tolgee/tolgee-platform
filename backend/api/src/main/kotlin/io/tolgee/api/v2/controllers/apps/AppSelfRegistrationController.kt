package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.request.apps.AppSelfRegisterRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.organization.apps.AppInstallModel
import io.tolgee.hateoas.organization.apps.AppInstallModelAssembler
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppRegistrationSecretService
import io.tolgee.service.organization.OrganizationRoleService
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Lets an app register itself against a running server, authenticating with the server-configured
 * registration secret instead of a signed-in user — so a first-party app deployed alongside Tolgee
 * can connect on boot, without anyone clicking through the UI. Which organization it registers into
 * is the app's own configuration ([AppSelfRegisterRequest.organizationSlug]), defaulting to the
 * server's initial organization.
 *
 * Public by path (the `/v2/public` namespace is permit-all): the request authenticates itself with
 * the registration secret, so it must not require a Tolgee session. The secret travels in a custom
 * header rather than `Authorization`, which the authentication filter would try to parse as a token.
 */
@RestController
@CrossOrigin(origins = ["*"])
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/public/apps"])
@Tag(name = "App Self-Registration")
class AppSelfRegistrationController(
  private val appInstallService: AppInstallService,
  private val appInstallModelAssembler: AppInstallModelAssembler,
  private val appRegistrationSecretService: AppRegistrationSecretService,
  private val organizationRoleService: OrganizationRoleService,
) {
  @PostMapping("/self-register")
  @RateLimited(5, isAuthentication = true)
  @Operation(
    summary = "Register an app using the server's registration secret",
    description =
      "Registers the app described by the manifest, without a signed-in user, authenticated by the " +
        "`X-Tolgee-App-Registration-Token` header matching the secret whose hash the server is " +
        "configured with (`tolgee.apps.registration-secret-hash`; endpoint refuses everything when " +
        "unset). The app registers into — and is owned by — the organization named by " +
        "`organizationSlug`, or the server's initial organization when omitted. Re-running it for " +
        "an already-registered app repoints it at the new manifest URL; the app-level credentials " +
        "are disclosed only when the app is first registered.",
  )
  fun selfRegister(
    @RequestHeader(REGISTRATION_TOKEN_HEADER, required = false) providedSecret: String?,
    @RequestBody @Valid body: AppSelfRegisterRequest,
  ): AppInstallModel {
    appRegistrationSecretService.authenticate(providedSecret)
    val organization = appRegistrationSecretService.resolveOrganization(body.organizationSlug)
    val author = resolveAuthor(organization)

    return appInstallModelAssembler.toModel(appInstallService.selfRegister(organization, body.manifestUrl, author))
  }

  /** The install is recorded as created by an owner of the organization it registers into. */
  private fun resolveAuthor(organization: Organization): UserAccount {
    return organizationRoleService.getOwners(organization).firstOrNull()
      ?: throw BadRequestException(Message.ORGANIZATION_HAS_NO_OWNER)
  }

  companion object {
    const val REGISTRATION_TOKEN_HEADER = "X-Tolgee-App-Registration-Token"
  }
}
