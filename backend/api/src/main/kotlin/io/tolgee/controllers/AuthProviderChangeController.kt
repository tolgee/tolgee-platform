package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.dtos.response.AuthProviderDto
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.security.AuthProviderChangeService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth_provider") // TODO: I should probably use the v2
@AuthenticationTag
class AuthProviderChangeController(
  private val authenticationFacade: AuthenticationFacade,
  private val authProviderChangeService: AuthProviderChangeService,
) {
  @PostMapping("/accept_change")
  @Operation(summary = "Accept change of the third party authentication provider")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @Transactional
  fun acceptChangeAuthProvider() {
    authProviderChangeService.acceptProviderChange(authenticationFacade.authenticatedUserEntity)
  }

  @PostMapping("/reject_change")
  @Operation(summary = "Reject change of the third party authentication provider")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @Transactional
  fun rejectChangeAuthProvider() {
    authProviderChangeService.rejectProviderChange(authenticationFacade.authenticatedUserEntity)
  }

  @GetMapping("/current")
  @Operation(summary = "Get current third party authentication provider")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  fun getCurrentAuthProvider(): AuthProviderDto? {
    return authProviderChangeService.getCurrent(authenticationFacade.authenticatedUserEntity)
  }

  @GetMapping("/changed")
  @Operation(summary = "Get info about authentication provider which can replace the current one")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  fun getChangedAuthProvider(): AuthProviderDto? {
    return authProviderChangeService.getRequestedChange(authenticationFacade.authenticatedUserEntity)
  }
}
