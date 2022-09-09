package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.UserMfaRecoveryRequestDto
import io.tolgee.dtos.request.UserTotpDisableRequestDto
import io.tolgee.dtos.request.UserTotpEnableRequestDto
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.patAuth.DenyPatAccess
import io.tolgee.service.MfaService
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/v2/user/mfa")
@Tag(name = "User Multi-Factor Authentication")
class UserMfaController(
  private val authenticationFacade: AuthenticationFacade,
  private val mfaService: MfaService
) {
  @PutMapping("/totp")
  @Operation(summary = "Enables TOTP-based two-factor authentication")
  @DenyPatAccess
  fun enableMfa(@RequestBody @Valid dto: UserTotpEnableRequestDto) {
    mfaService.enableTotpFor(authenticationFacade.userAccountEntity, dto)
  }

  @DeleteMapping("/totp")
  @Operation(summary = "Disables TOTP-based two-factor authentication")
  @DenyPatAccess
  fun disableMfa(@RequestBody @Valid dto: UserTotpDisableRequestDto) {
    mfaService.disableTotpFor(authenticationFacade.userAccountEntity, dto)
  }

  @PutMapping("/recovery")
  @Operation(summary = "Regenerates multi-factor authentication recovery codes")
  @DenyPatAccess
  fun regenerateRecoveryCodes(@RequestBody @Valid dto: UserMfaRecoveryRequestDto): List<String> {
    return mfaService.regenerateRecoveryCodes(authenticationFacade.userAccountEntity, dto)
  }
}
