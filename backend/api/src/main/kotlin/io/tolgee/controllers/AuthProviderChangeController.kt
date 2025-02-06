package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.dtos.response.AuthProviderDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.security.AuthProviderChangeService
import io.tolgee.service.security.UserAccountService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/auth_provider") // TODO: I should probably use the v2
@AuthenticationTag
class AuthProviderChangeController(
  private val authenticationFacade: AuthenticationFacade,
  private val authProviderChangeService: AuthProviderChangeService,
  private val userAccountService: UserAccountService,
  private val jwtService: JwtService,
) {
  @GetMapping("/current")
  @Operation(summary = "Get current third party authentication provider")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  fun getCurrentAuthProvider(): AuthProviderDto {
    val info = authProviderChangeService.getCurrent(authenticationFacade.authenticatedUserEntity)
    return info ?: throw NotFoundException()
  }

  @GetMapping("/changed")
  @Operation(summary = "Get info about authentication provider which can replace the current one")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  fun getChangedAuthProvider(): AuthProviderDto {
    val info = authProviderChangeService.getRequestedChange(authenticationFacade.authenticatedUserEntity)
    return info ?: throw NotFoundException()
  }

  @PostMapping("/changed/accept")
  @Operation(summary = "Accept change of the third party authentication provider")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresSuperAuthentication
  @Transactional
  fun acceptChangeAuthProvider(): JwtAuthenticationResponse {
    val user = authenticationFacade.authenticatedUserEntity
    authProviderChangeService.acceptProviderChange(user)
    userAccountService.invalidateTokens(user)
    return JwtAuthenticationResponse(
      jwtService.emitToken(authenticationFacade.authenticatedUser.id, true),
    )
  }

  @PostMapping("/changed/reject")
  @Operation(summary = "Reject change of the third party authentication provider")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @Transactional
  fun rejectChangeAuthProvider() {
    authProviderChangeService.rejectProviderChange(authenticationFacade.authenticatedUserEntity)
  }
}
