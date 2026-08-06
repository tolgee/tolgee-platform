package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.apps.AppClientCredentialsRequest
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.apps.AppAccessTokenModel
import io.tolgee.model.apps.App
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.apps.AppInstallSecretService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppSecretService
import io.tolgee.service.apps.AppService
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
  private val appInstallService: AppInstallService,
  private val appInstallSecretService: AppInstallSecretService,
  private val appService: AppService,
  private val appSecretService: AppSecretService,
  private val appTokenService: AppTokenService,
  private val tolgeeProperties: TolgeeProperties,
) {
  @PostMapping("/token")
  // Every process of a scaled app mints its own token and re-mints hourly, so a deploy that
  // restarts every replica at once arrives here as a burst.
  @RateLimited(120, isAuthentication = true)
  @Operation(
    summary = "Exchange app client credentials for an access token",
    description =
      "OAuth 2.0 client-credentials grant. Returns a short-lived install-context access token the " +
        "app's backend uses to call Tolgee's REST API as the install.",
  )
  fun token(
    @RequestBody @Valid body: AppClientCredentialsRequest,
  ): AppAccessTokenModel {
    if (body.grantType != GRANT_TYPE_CLIENT_CREDENTIALS) {
      throw BadRequestException(Message.APP_UNSUPPORTED_GRANT_TYPE)
    }

    val installId = authenticateForInstall(body)
    val token = appTokenService.mintInstallContextToken(installId)
    return AppAccessTokenModel(
      accessToken = token,
      tokenType = "Bearer",
      expiresIn = tolgeeProperties.apps.tokenExpiration / 1000,
    )
  }

  /**
   * The credentials may be either the app's own or one install's, told apart by the `client_id`
   * prefix. App-level credentials identify an app that many organizations installed, so they carry
   * no install of their own and the caller has to name one.
   */
  private fun authenticateForInstall(body: AppClientCredentialsRequest): Long {
    appService.resolveByClientId(body.clientId)?.let { return appCredentialsInstall(it, body) }
    return installCredentialsInstall(body)
  }

  private fun appCredentialsInstall(
    app: App,
    body: AppClientCredentialsRequest,
  ): Long {
    val secret =
      appSecretService.findLiveMatching(app.id, body.clientSecret)
        ?: throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)

    appSecretService.updateLastUsedAsync(secret.id, secret.lastUsedAt)

    val installId = body.installId ?: throw BadRequestException(Message.APP_INSTALL_ID_REQUIRED)
    val install =
      appInstallService.findOwnInstall(app.id, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)

    return install.id
  }

  private fun installCredentialsInstall(body: AppClientCredentialsRequest): Long {
    val install =
      appInstallService.resolveByClientId(body.clientId)
        ?: throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)

    val secret =
      appInstallSecretService.findLiveMatching(install.id, body.clientSecret)
        ?: throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)

    appInstallSecretService.updateLastUsedAsync(secret.id, secret.lastUsedAt)

    return install.id
  }

  companion object {
    private const val GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials"
  }
}
