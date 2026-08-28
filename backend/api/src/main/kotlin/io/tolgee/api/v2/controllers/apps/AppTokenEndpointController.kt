package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.apps.AppClientCredentialsRequest
import io.tolgee.hateoas.apps.AppAccessTokenModel
import io.tolgee.security.authentication.AppAccessNeutral
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.apps.AppTokenGrantService
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@AppAccessNeutral
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/public/apps"])
@Tag(name = "App Authentication")
class AppTokenEndpointController(
  private val appTokenGrantService: AppTokenGrantService,
  private val tolgeeProperties: TolgeeProperties,
) {
  @PostMapping("/token")
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
    val token =
      appTokenGrantService.issueFromClientCredentials(
        grantType = body.grantType,
        clientId = body.clientId,
        clientSecret = body.clientSecret,
        installId = body.installId,
      )
    return AppAccessTokenModel(
      accessToken = token,
      tokenType = "Bearer",
      expiresIn = tolgeeProperties.apps.tokenExpiration / 1000,
    )
  }
}
