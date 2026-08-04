package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.ee.api.v2.hateoas.assemblers.UserSessionModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.UserSessionModel
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.UserSession
import io.tolgee.model.enums.UserSessionType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.security.authentication.ReadOnlyOperation
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
  @GetMapping(value = [""])
  @Operation(summary = "Get active sessions of the current user")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
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

  /**
   * Revoking your own session only ever reduces your own access, so it stays available to a
   * read-only (supporter) principal - unlike the other two.
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
   * Impersonation sessions are reported as missing rather than forbidden, so that probing by id
   * cannot reveal the support access the listing hides.
   */
  private fun checkOwner(id: Long): UserSession {
    val session = userSessionService.find(id) ?: throw NotFoundException()
    if (session.type == UserSessionType.IMPERSONATION) {
      throw NotFoundException()
    }
    if (session.userAccountId != authenticationFacade.authenticatedUser.id) {
      throw PermissionException()
    }
    return session
  }
}
