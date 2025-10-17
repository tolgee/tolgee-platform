package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.UserMfaRecoveryRequestDto
import io.tolgee.dtos.request.UserTotpDisableRequestDto
import io.tolgee.dtos.request.UserTotpEnableRequestDto
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.security.MfaService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/user/mfa")
@Tag(name = "User Multi-Factor Authentication")
class UserMfaController(
  private val authenticationFacade: AuthenticationFacade,
  private val mfaService: MfaService,
  private val jwtService: JwtService,
) {
  @PutMapping("/totp")
  @Operation(
    summary = "Enable TOTP",
    description = "Enables TOTP-based two-factor authentication. Invalidates all previous sessions upon success.",
  )
  @BypassEmailVerification
  fun enableMfa(
    @RequestBody @Valid
    dto: UserTotpEnableRequestDto,
  ): JwtAuthenticationResponse {
    mfaService.enableTotpFor(authenticationFacade.authenticatedUserEntity, dto)
    return JwtAuthenticationResponse(
      jwtService.emitTokenRefreshForCurrentUser(isSuper = true),
    )
  }

  @DeleteMapping("/totp")
  @Operation(
    summary = "Disable TOTP",
    description = "Disables TOTP-based two-factor authentication. Invalidates all previous sessions upon success.",
  )
  @BypassEmailVerification
  fun disableMfa(
    @RequestBody @Valid
    dto: UserTotpDisableRequestDto,
  ): JwtAuthenticationResponse {
    mfaService.disableTotpFor(authenticationFacade.authenticatedUserEntity, dto)
    return JwtAuthenticationResponse(
      jwtService.emitTokenRefreshForCurrentUser(isSuper = true),
    )
  }

  @PutMapping("/recovery")
  @Operation(summary = "Regenerate Codes", description = "Regenerates multi-factor authentication recovery codes")
  @BypassEmailVerification
  fun regenerateRecoveryCodes(
    @RequestBody @Valid
    dto: UserMfaRecoveryRequestDto,
  ): List<String> {
    return mfaService.regenerateRecoveryCodes(authenticationFacade.authenticatedUserEntity, dto)
  }
}
