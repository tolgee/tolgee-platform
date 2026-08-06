package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.apps.AppSelfRegisterRequest
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.organization.apps.AppInstallModel
import io.tolgee.hateoas.organization.apps.AppInstallModelAssembler
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.constantTimeEquals
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Lets an app register itself against a running server, authenticating with the server-wide
 * `tolgee.apps.registration-secret` instead of a signed-in user — so an app can be connected without
 * restarting Tolgee or clicking through the UI.
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
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService,
  private val tolgeeProperties: TolgeeProperties,
) {
  @PostMapping("/self-register")
  @RateLimited(5, isAuthentication = true)
  @Operation(
    summary = "Register an app using the server-wide registration secret",
    description =
      "Registers the app described by the manifest, without a signed-in user. Requires the " +
        "`X-Tolgee-App-Registration-Secret` header to match `tolgee.apps.registration-secret`. " +
        "With an `organizationSlug` the app is installed into that organization; without one it is " +
        "registered as a native (server-level) app that a server admin then makes available to " +
        "organizations. Re-running it for an already-registered app repoints it at the new manifest " +
        "URL; the one-time client secret is returned only when the install is first created.",
  )
  fun selfRegister(
    @RequestHeader(REGISTRATION_SECRET_HEADER, required = false) providedSecret: String?,
    @RequestBody @Valid body: AppSelfRegisterRequest,
  ): AppInstallModel {
    verifySecret(providedSecret)

    val organization = resolveOrganization(body.organizationSlug)
    val author = resolveAuthor(organization)

    val result = appInstallService.selfRegister(organization, body.manifestUrl, author)

    val secret = result.plaintextClientSecret
    if (secret == null) {
      return appInstallModelAssembler.toModel(result.install)
    }
    return appInstallModelAssembler.toModelWithSecret(result.install, secret)
  }

  private fun resolveOrganization(slug: String?): Organization? {
    if (slug.isNullOrBlank()) return null
    return organizationService.find(slug) ?: throw NotFoundException(Message.ORGANIZATION_NOT_FOUND)
  }

  /**
   * A native install has no organization to draw an owner from, so it is attributed to the server's
   * initial user — the install author doubles as the principal an install-context token acts as.
   */
  private fun resolveAuthor(organization: Organization?): UserAccount {
    if (organization == null) {
      return userAccountService.findInitialUser() ?: throw BadRequestException(Message.INITIAL_USER_NOT_FOUND)
    }
    return organizationRoleService.getOwners(organization).firstOrNull()
      ?: throw BadRequestException(Message.ORGANIZATION_HAS_NO_OWNER)
  }

  private fun verifySecret(providedSecret: String?) {
    val configured = tolgeeProperties.apps.registrationSecret
    if (configured.isNullOrBlank()) {
      throw BadRequestException(Message.APP_SELF_REGISTRATION_DISABLED)
    }
    if (providedSecret == null || !constantTimeEquals(providedSecret, configured)) {
      throw AuthenticationException(Message.INVALID_APP_REGISTRATION_SECRET)
    }
  }

  companion object {
    const val REGISTRATION_SECRET_HEADER = "X-Tolgee-App-Registration-Secret"
  }
}
