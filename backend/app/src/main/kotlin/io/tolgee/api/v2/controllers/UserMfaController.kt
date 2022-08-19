package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.UserMfaRecoveryRequestDto
import io.tolgee.dtos.request.UserTotpDisableRequestDto
import io.tolgee.dtos.request.UserTotpEnableRequestDto
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.MfaService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/v2/user/mfa")
@Tag(name = "User Multi-Factor Authentication")
class UserMfaController(
  private val authenticationFacade: AuthenticationFacade,
  private val mfaService: MfaService
) {
  @PostMapping("/mfa/totp")
  @Operation(summary = "Enables TOTP-based two-factor authentication")
  fun enableMfa(@RequestBody @Valid dto: UserTotpEnableRequestDto): ResponseEntity<String> {
    mfaService.enableTotpFor(authenticationFacade.userAccountEntity, dto)
    return ResponseEntity.noContent().build()
  }

  @DeleteMapping("/mfa/totp")
  @Operation(summary = "Disables TOTP-based two-factor authentication")
  fun disableMfa(@RequestBody @Valid dto: UserTotpDisableRequestDto): ResponseEntity<String> {
    mfaService.disableTotpFor(authenticationFacade.userAccountEntity, dto)
    return ResponseEntity.noContent().build()
  }

  @PostMapping("/mfa/recovery")
  @Operation(summary = "Regenerates multi-factor authentication recovery keys")
  fun regenerateRecoveryKeys(@RequestBody @Valid dto: UserMfaRecoveryRequestDto): ResponseEntity<List<String>> {
    val codes = mfaService.regenerateRecoveryCodes(authenticationFacade.userAccountEntity, dto)
    return ResponseEntity(codes, HttpStatus.OK)
  }
}
