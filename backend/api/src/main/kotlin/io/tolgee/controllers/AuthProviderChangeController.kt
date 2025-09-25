package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.dtos.request.auth.AcceptAuthProviderChangeRequest
import io.tolgee.dtos.response.AuthProviderDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.security.AuthProviderChangeService
import io.tolgee.service.security.UserAccountService
import jakarta.validation.Valid
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/auth-provider")
@AuthenticationTag
@OpenApiHideFromPublicDocs
class AuthProviderChangeController(
  private val authenticationFacade: AuthenticationFacade,
  private val authProviderChangeService: AuthProviderChangeService,
  private val userAccountService: UserAccountService,
  private val jwtService: JwtService,
) {
  @GetMapping("")
  @Operation(summary = "Get current third party authentication provider")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun getCurrentAuthProvider(): AuthProviderDto {
    val info = authProviderChangeService.getCurrent(authenticationFacade.authenticatedUserEntity)
    return info ?: throw NotFoundException()
  }

  @DeleteMapping("")
  @Operation(summary = "Initiate provider change to remove current third party authentication provider")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @BypassForcedSsoAuthentication
  @RequiresSuperAuthentication
  @Transactional
  fun deleteCurrentAuthProvider() {
    val user = authenticationFacade.authenticatedUserEntity
    authProviderChangeService.initiateRemove(user)
  }

  @GetMapping("/change")
  @Operation(summary = "Get info about authentication provider which can replace the current one")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @BypassForcedSsoAuthentication
  fun getChangedAuthProvider(): AuthProviderDto {
    val info = authProviderChangeService.getRequestedChange(authenticationFacade.authenticatedUserEntity)
    return info ?: throw NotFoundException()
  }

  @PostMapping("/change")
  @Operation(summary = "Accept change of the third party authentication provider")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @BypassForcedSsoAuthentication
  @RequiresSuperAuthentication
  @Transactional
  fun acceptChangeAuthProvider(
    @RequestBody @Valid
    request: AcceptAuthProviderChangeRequest,
  ): JwtAuthenticationResponse {
    val user = authenticationFacade.authenticatedUserEntity
    authProviderChangeService.accept(user, request.id)
    userAccountService.invalidateTokens(user)
    return JwtAuthenticationResponse(jwtService.emitTokenRefreshForCurrentUser(isSuper = true))
  }

  @DeleteMapping("/change")
  @Operation(summary = "Reject change of the third party authentication provider")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @BypassForcedSsoAuthentication
  @Transactional
  fun rejectChangeAuthProvider() {
    authProviderChangeService.reject(authenticationFacade.authenticatedUserEntity)
  }
}
