package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.KeyGenerator
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.apps.AppClientCredentialsRequest
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.apps.AppAccessTokenModel
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.apps.AppInstallService
import io.tolgee.util.constantTimeEquals
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
  private val appTokenService: AppTokenService,
  private val keyGenerator: KeyGenerator,
  private val tolgeeProperties: TolgeeProperties,
) {
  @PostMapping("/token")
  @RateLimited(5, isAuthentication = true)
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

    val resolution =
      appInstallService.resolveByClientId(body.clientId)
        ?: throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)

    val storedHash = resolution.install.clientSecretHash
    val providedHash = keyGenerator.hash(body.clientSecret)
    if (storedHash == null || !constantTimeEquals(providedHash, storedHash)) {
      throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
    }

    val token = appTokenService.mintInstallContextToken(resolution.install.id)
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
