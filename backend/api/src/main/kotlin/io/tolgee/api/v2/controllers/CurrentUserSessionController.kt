package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.service.security.UserSessionService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Signing out has to revoke the session in every build, so it lives in core - unlike listing and
 * revoking *other* sessions, which is the enterprise feature.
 */
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/user/sessions")
@Tag(name = "User sessions")
class CurrentUserSessionController(
  private val userSessionService: UserSessionService,
  private val authenticationFacade: AuthenticationFacade,
) : IController {
  /**
   * Revoking your own session only ever reduces your own access, so it stays available to a
   * read-only (supporter) principal.
   */
  @DeleteMapping(value = ["/current"])
  @Operation(summary = "Revoke the session of the current token")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  @ReadOnlyOperation
  fun revokeCurrent() {
    userSessionService.revokeCurrent(
      userAccountId = authenticationFacade.authenticatedUser.id,
      deviceId = authenticationFacade.deviceIdOrNull,
      revokedById = authenticationFacade.authenticatedUser.id,
    )
  }
}
