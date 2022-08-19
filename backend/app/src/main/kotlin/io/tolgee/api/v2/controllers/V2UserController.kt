package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.user_account.UserAccountModel
import io.tolgee.api.v2.hateoas.user_account.UserAccountModelAssembler
import io.tolgee.constants.Message
import io.tolgee.dtos.request.UserMfaRecoveryRequestDto
import io.tolgee.dtos.request.UserTotpDisableRequestDto
import io.tolgee.dtos.request.UserTotpEnableRequestDto
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.ImageUploadService
import io.tolgee.service.MfaService
import io.tolgee.service.UserAccountService
import org.apache.commons.codec.binary.Base32
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import javax.validation.Valid

@RestController
@RequestMapping("/v2/user")
@Tag(name = "User", description = "Manipulates currently authorized user")
class V2UserController(
  private val authenticationFacade: AuthenticationFacade,
  private val userAccountService: UserAccountService,
  private val userAccountModelAssembler: UserAccountModelAssembler,
  private val imageUploadService: ImageUploadService,
  private val mfaService: MfaService
) {
  private val base32 = Base32()

  @Operation(summary = "Returns current user's data")
  @GetMapping("")
  fun getInfo(): UserAccountModel {
    val userAccount = authenticationFacade.userAccountEntity
    return userAccountModelAssembler.toModel(userAccount)
  }

  @PostMapping("")
  @Operation(summary = "Updates current user's data")
  fun updateUser(@RequestBody @Valid dto: UserUpdateRequestDto?): UserAccountModel {
    val userAccount = userAccountService.update(authenticationFacade.userAccountEntity, dto!!)
    return userAccountModelAssembler.toModel(userAccount)
  }

  @PutMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Uploads user's avatar")
  @ResponseStatus(HttpStatus.OK)
  fun uploadAvatar(
    @RequestParam("avatar") avatar: MultipartFile,
  ): UserAccountModel {
    imageUploadService.validateIsImage(avatar)
    val entity = authenticationFacade.userAccountEntity
    userAccountService.setAvatar(authenticationFacade.userAccountEntity, avatar.inputStream)
    return userAccountModelAssembler.toModel(entity)
  }

  @DeleteMapping("/avatar")
  @Operation(summary = "Deletes user's avatar")
  @ResponseStatus(HttpStatus.OK)
  fun removeAvatar(): UserAccountModel {
    val entity = authenticationFacade.userAccountEntity
    userAccountService.removeAvatar(authenticationFacade.userAccountEntity)
    return userAccountModelAssembler.toModel(entity)
  }

  @PostMapping("/mfa/totp")
  @Operation(summary = "Enables TOTP-based two-factor authentication")
  fun enableMfa(@RequestBody @Valid dto: UserTotpEnableRequestDto): ResponseEntity<String> {
    if (authenticationFacade.userAccountEntity.totpKey?.isNotEmpty() == true) {
      throw AuthenticationException(Message.MFA_ENABLED)
    }

    if (dto.totpKey.length != 16 || !base32.isInAlphabet(dto.totpKey)) {
      throw BadRequestException(Message.INVALID_OTP_KEY)
    }

    val bCryptPasswordEncoder = BCryptPasswordEncoder()
    val matches = bCryptPasswordEncoder.matches(dto.password, authenticationFacade.userAccountEntity.password)
    if (!matches) {
      throw AuthenticationException(Message.BAD_CREDENTIALS)
    }

    val key = Base32().decode(dto.totpKey)
    if (!mfaService.validateOtpCode(dto.otp, key)) {
      throw AuthenticationException(Message.INVALID_OTP_CODE)
    }

    userAccountService.enableMfaTotp(authenticationFacade.userAccountEntity, key)
    return ResponseEntity.noContent().build()
  }

  @DeleteMapping("/mfa/totp")
  @Operation(summary = "Disables TOTP-based two-factor authentication")
  fun disableMfa(@RequestBody @Valid dto: UserTotpDisableRequestDto): ResponseEntity<String> {
    if (authenticationFacade.userAccountEntity.totpKey?.isNotEmpty() != true) {
      throw AuthenticationException(Message.MFA_NOT_ENABLED)
    }

    if (!mfaService.validateOtpCode(dto.otp, authenticationFacade.userAccountEntity)) {
      throw AuthenticationException(Message.INVALID_OTP_CODE)
    }

    userAccountService.disableMfaTotp(authenticationFacade.userAccountEntity)
    return ResponseEntity.noContent().build()
  }

  @PostMapping("/mfa/recovery")
  @Operation(summary = "Regenerates multi-factor authentication recovery keys")
  fun regenerateRecoveryKeys(@RequestBody @Valid dto: UserMfaRecoveryRequestDto): ResponseEntity<List<String>> {
    // note: if support for more MFA methods is added, this check should be reworked to account for them.
    if (authenticationFacade.userAccountEntity.totpKey?.isNotEmpty() != true) {
      throw AuthenticationException(Message.MFA_NOT_ENABLED)
    }

    if (!mfaService.validateOtpCode(dto.otp, authenticationFacade.userAccountEntity)) {
      throw AuthenticationException(Message.INVALID_OTP_CODE)
    }

    val codes = mfaService.generateRecoveryKeys()
    userAccountService.regenerateMfaRecoveryCodes(authenticationFacade.userAccountEntity, codes)
    return ResponseEntity.ok(codes)
  }
}
