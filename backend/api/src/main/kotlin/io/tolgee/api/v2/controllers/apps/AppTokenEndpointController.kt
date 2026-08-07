package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.apps.AppClientCredentialsRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.apps.AppAccessTokenModel
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
 * OAuth 2.0 client-credentials token endpoint (RFC 6749 §4.4) for Tolgee Apps. An app's backend
 * posts its `client_id` + `client_secret` and receives a short-lived install-context access token,
 * so the raw secret only ever travels to this endpoint — subsequent API calls carry the token.
 *
 * Public by path (the `/v2/public` namespace is permit-all): the request authenticates itself with
 * the client credentials, so it must not require an existing Tolgee session.
 */
@RestController
@CrossOrigin(origins = ["*"])
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
        "uses to call Tolgee's REST API as that install. Install ids come from " +
        "`POST /v2/public/apps/installations/list`.",
  )
  fun token(
    @RequestBody @Valid body: AppClientCredentialsRequest,
  ): AppAccessTokenModel {
    if (body.grantType != GRANT_TYPE_CLIENT_CREDENTIALS) {
      throw BadRequestException(Message.APP_UNSUPPORTED_GRANT_TYPE)
    }

    val app = appCredentialAuthenticator.authenticate(body.clientId, body.clientSecret)

    // App-level credentials identify an app installed by any number of organizations, so they
    // cannot imply an install on their own.
    val installId = body.installId ?: throw BadRequestException(Message.APP_INSTALL_ID_REQUIRED)
    val install =
      appInstallService.findOwnInstall(app.id, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)

    val token = appTokenService.mintInstallContextToken(install.id)
    return AppAccessTokenModel(
      accessToken = token,
      tokenType = "Bearer",
      expiresIn = tolgeeProperties.apps.tokenExpiration / 1000,
    )
  }

  companion object {
    private const val GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials"
  }
}
