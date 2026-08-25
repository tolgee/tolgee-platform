package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.request.apps.AppCredentialsRequest
import io.tolgee.dtos.request.apps.AppSecretRevokeRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.apps.AppSecretModel
import io.tolgee.hateoas.apps.AppSecretModelAssembler
import io.tolgee.security.authentication.AppAccessNeutral
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.apps.AppCredentialAuthenticator
import io.tolgee.service.apps.AppSecretService
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Lets a published app manage its own app-level secrets unattended. Authenticates with the app-level
 * credentials themselves, not a token — those credentials never become a session, and nothing here
 * touches anything but the app's own secrets.
 */
@RestController
@CrossOrigin(origins = ["*"])
@AppAccessNeutral
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/public/apps/app-secrets"])
@Tag(name = "App Self Service")
class AppSelfAppSecretsController(
  private val appCredentialAuthenticator: AppCredentialAuthenticator,
  private val appSecretService: AppSecretService,
  private val appSecretModelAssembler: AppSecretModelAssembler,
) {
  @PostMapping("/list")
  @RateLimited(5, isAuthentication = true)
  @Operation(
    summary = "List the calling app's own app-level secrets",
    description = "Returns every app-level secret, revoked ones included, without disclosing any of them.",
  )
  fun list(
    @RequestBody @Valid body: AppCredentialsRequest,
  ): CollectionModel<AppSecretModel> {
    val app = appCredentialAuthenticator.authenticate(body.clientId, body.clientSecret)
    return appSecretModelAssembler.toCollectionModel(appSecretService.list(app.id))
  }

  @PostMapping("/issue")
  @RateLimited(5, isAuthentication = true)
  @Operation(
    summary = "Issue an additional app-level client secret for the calling app",
    description =
      "Mints a fresh app-level secret and returns it — the only place it is ever disclosed. The " +
        "secret this call authenticated with keeps working, so the app can store the new one and " +
        "only then revoke the old one.",
  )
  fun issue(
    @RequestBody @Valid body: AppCredentialsRequest,
  ): AppSecretModel {
    val app = appCredentialAuthenticator.authenticate(body.clientId, body.clientSecret)
    val issued = appSecretService.issue(app)
    return appSecretModelAssembler.toModelWithSecret(issued.secret, issued.plaintextSecret)
  }

  @PostMapping("/revoke")
  @RateLimited(5, isAuthentication = true)
  @Operation(
    summary = "Revoke one of the calling app's own app-level secrets",
    description =
      "The secret stops authenticating immediately. Revoking the app's only active secret is " +
        "refused, so an app cannot lock itself out of this very endpoint — issue the replacement " +
        "first, then revoke the old one. Idempotent.",
  )
  fun revoke(
    @RequestBody @Valid body: AppSecretRevokeRequest,
  ): AppSecretModel {
    val app = appCredentialAuthenticator.authenticate(body.clientId, body.clientSecret)
    val secretId = body.secretId ?: throw BadRequestException(Message.APP_SECRET_ID_REQUIRED)
    return appSecretModelAssembler.toModel(appSecretService.revoke(app.id, secretId, force = false))
  }
}
