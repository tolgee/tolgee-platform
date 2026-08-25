package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.apps.AppClientCredentialsRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.apps.AppAccessTokenModel
import io.tolgee.model.apps.App
import io.tolgee.security.authentication.AppAccessNeutral
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.apps.AppCredentialAuthenticator
import io.tolgee.service.apps.AppInstallService
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Client-credentials token endpoint. Modelled on the OAuth 2.0 client-credentials grant (RFC 6749
 * §4.4) but JSON-encoded with a Tolgee error envelope, so use Tolgee's app SDK, not a stock OAuth
 * library. An app's backend exchanges its `client_id` + `client_secret` for a short-lived
 * install-context token, so subsequent API calls carry the token rather than the raw secret.
 */
@RestController
@CrossOrigin(origins = ["*"])
@AppAccessNeutral
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/public/apps"])
@Tag(name = "App Authentication")
class AppTokenEndpointController(
  private val appCredentialAuthenticator: AppCredentialAuthenticator,
  private val appInstallService: AppInstallService,
  private val appTokenService: AppTokenService,
  private val tolgeeProperties: TolgeeProperties,
) {
  @PostMapping("/token")
  // Every process of a scaled app mints its own token and re-mints hourly, so a deploy that
  // restarts every replica at once arrives here as a burst.
  @RateLimited(120, isAuthentication = true)
  @Operation(
    summary = "Exchange app client credentials for an install-scoped access token",
    description =
      "OAuth 2.0 client-credentials grant. Authenticates with the app-level credentials, names an " +
        "installation via `install_id`, and returns a short-lived access token the app's backend " +
        "uses to call Tolgee's REST API as that install. Omit `install_id` to get an app-level token " +
        "for app-level operations (installation discovery); the install ids come from there.",
  )
  fun token(
    @RequestBody @Valid body: AppClientCredentialsRequest,
  ): AppAccessTokenModel {
    if (body.grantType != GRANT_TYPE_CLIENT_CREDENTIALS) {
      throw BadRequestException(Message.APP_UNSUPPORTED_GRANT_TYPE)
    }

    val app = appCredentialAuthenticator.authenticate(body.clientId, body.clientSecret)

    val token = mintToken(app, body.installId)
    return AppAccessTokenModel(
      accessToken = token,
      tokenType = "Bearer",
      expiresIn = tolgeeProperties.apps.tokenExpiration / 1000,
    )
  }

  private fun mintToken(
    app: App,
    installId: Long?,
  ): String {
    if (installId == null) return appTokenService.mintAppLevelToken(app.id)
    val install =
      appInstallService.findOwnInstall(app.id, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    return appTokenService.mintInstallContextToken(install.id)
  }

  companion object {
    private const val GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials"
  }
}
