package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.ee.api.v2.hateoas.assemblers.UserSessionModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.UserSessionModel
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserSession
import io.tolgee.model.enums.UserSessionType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.service.security.UserSessionService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/user/sessions")
@Tag(name = "User sessions")
class UserSessionsController(
  private val userSessionService: UserSessionService,
  private val userSessionModelAssembler: UserSessionModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedResourcesAssembler: PagedResourcesAssembler<UserSession>,
  private val authenticationFacade: AuthenticationFacade,
) : IController {
  /**
   * Unlike the revoke endpoints, this one is gated: it is the only place in the API that discloses
   * the approximate physical location a person signs in from, as a history across their devices, so
   * a stolen token must not be enough to harvest it. Revoking stays ungated on purpose - a password
   * prompt does not belong between someone and ejecting an intruder.
   */
  @GetMapping(value = [""])
  @Operation(summary = "Get active sessions of the current user")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  @RequiresSuperAuthentication
  fun getAll(
    @ParameterObject pageable: Pageable,
  ): PagedModel<UserSessionModel> {
    val sessions =
      userSessionService.findActive(
        userAccountId = authenticationFacade.authenticatedUser.id,
        tokensValidNotBefore = authenticationFacade.authenticatedUser.tokensValidNotBefore,
        pageable = pageable,
      )
    return pagedResourcesAssembler.toModel(sessions, userSessionModelAssembler)
  }

  @DeleteMapping(value = ["/{id:[0-9]+}"])
  @Operation(summary = "Revoke a session")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun revoke(
    @PathVariable id: Long,
  ) {
    val session = checkOwner(id)
    userSessionService.revoke(session, authenticationFacade.authenticatedUser.id)
  }

  @DeleteMapping(value = ["/other"])
  @Operation(summary = "Revoke all sessions except the current one")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun revokeAllOthers() {
    userSessionService.revokeAllOthers(
      userAccountId = authenticationFacade.authenticatedUser.id,
      currentDeviceId = authenticationFacade.deviceIdOrNull,
      revokedById = authenticationFacade.authenticatedUser.id,
    )
  }

  /**
   * Everything the caller may not act on is reported as missing rather than forbidden: a distinct
   * 403 would turn this endpoint into an oracle for which session ids exist on other accounts, and
   * would reveal the impersonation sessions the listing deliberately hides.
   */
  private fun checkOwner(id: Long): UserSession {
    val session = userSessionService.find(id) ?: throw NotFoundException()
    if (session.type == UserSessionType.IMPERSONATION) {
      throw NotFoundException()
    }
    if (session.userAccountId != authenticationFacade.authenticatedUser.id) {
      throw NotFoundException()
    }
    return session
  }
}
