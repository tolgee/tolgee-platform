package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.request.SuperTokenRequest
import io.tolgee.dtos.request.UserUpdatePasswordRequestDto
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.hateoas.organization.SimpleOrganizationModel
import io.tolgee.hateoas.organization.SimpleOrganizationModelAssembler
import io.tolgee.hateoas.user_account.PrivateUserAccountModel
import io.tolgee.hateoas.user_account.PrivateUserAccountModelAssembler
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.JwtTokenProvider
import io.tolgee.security.NeedsSuperJwtToken
import io.tolgee.security.patAuth.DenyPatAccess
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.ImageUploadService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.MfaService
import io.tolgee.service.security.UserAccountService
import org.springframework.hateoas.CollectionModel
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.validation.Valid

@RestController
@RequestMapping("/v2/user")
@Tag(name = "User", description = "Manipulates currently authenticated user")
class V2UserController(
  private val authenticationFacade: AuthenticationFacade,
  private val userAccountService: UserAccountService,
  private val privateUserAccountModelAssembler: PrivateUserAccountModelAssembler,
  private val imageUploadService: ImageUploadService,
  private val organizationService: OrganizationService,
  private val simpleOrganizationModelAssembler: SimpleOrganizationModelAssembler,
  private val passwordEncoder: PasswordEncoder,
  private val jwtTokenProvider: JwtTokenProvider,
  private val mfaService: MfaService
) {
  @Operation(summary = "Returns current user's data.")
  @GetMapping("")
  fun getInfo(): PrivateUserAccountModel {
    val userAccount = authenticationFacade.userAccountEntity
    return privateUserAccountModelAssembler.toModel(userAccount)
  }

  @Operation(summary = "Deletes current user.")
  @DeleteMapping("")
  @NeedsSuperJwtToken
  fun delete() {
    userAccountService.delete(authenticationFacade.userAccountEntity)
  }

  @PostMapping("")
  @Operation(summary = "Updates current user's data.", deprecated = true)
  @DenyPatAccess
  fun updateUserOld(@RequestBody @Valid dto: UserUpdateRequestDto?): PrivateUserAccountModel = updateUser(dto)

  @PutMapping("")
  @Operation(summary = "Updates current user's data.")
  @DenyPatAccess
  fun updateUser(@RequestBody @Valid dto: UserUpdateRequestDto?): PrivateUserAccountModel {
    val userAccount = userAccountService.update(authenticationFacade.userAccountEntity, dto!!)
    return privateUserAccountModelAssembler.toModel(userAccount)
  }

  @PutMapping("/password")
  @Operation(summary = "Updates current user's password. Invalidates all previous sessions upon success.")
  @DenyPatAccess
  fun updateUserPassword(@RequestBody @Valid dto: UserUpdatePasswordRequestDto?): JwtAuthenticationResponse {
    userAccountService.updatePassword(authenticationFacade.userAccountEntity, dto!!)
    return JwtAuthenticationResponse(
      jwtTokenProvider.generateToken(authenticationFacade.userAccountEntity.id).toString()
    )
  }

  @PutMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Uploads user's avatar.")
  @ResponseStatus(HttpStatus.OK)
  @DenyPatAccess
  fun uploadAvatar(
    @RequestParam("avatar") avatar: MultipartFile,
  ): PrivateUserAccountModel {
    imageUploadService.validateIsImage(avatar)
    val entity = authenticationFacade.userAccountEntity
    userAccountService.setAvatar(authenticationFacade.userAccountEntity, avatar.inputStream)
    return privateUserAccountModelAssembler.toModel(entity)
  }

  @DeleteMapping("/avatar")
  @Operation(summary = "Deletes user's avatar.")
  @ResponseStatus(HttpStatus.OK)
  @DenyPatAccess
  fun removeAvatar(): PrivateUserAccountModel {
    val entity = authenticationFacade.userAccountEntity
    userAccountService.removeAvatar(authenticationFacade.userAccountEntity)
    return privateUserAccountModelAssembler.toModel(entity)
  }

  @GetMapping("/single-owned-organizations")
  @Operation(summary = "Returns all organizations owned only by current user")
  @ResponseStatus(HttpStatus.OK)
  @DenyPatAccess
  fun getAllSingleOwnedOrganizations(): CollectionModel<SimpleOrganizationModel> {
    val organizations = organizationService.getAllSingleOwnedByUser(authenticationFacade.userAccountEntity)
    return simpleOrganizationModelAssembler.toCollectionModel(organizations)
  }

  @PostMapping("/generate-super-token")
  @Operation(summary = "Generates new JWT token permitted to sensitive operations")
  @DenyPatAccess
  fun getSuperToken(@RequestBody @Valid req: SuperTokenRequest): ResponseEntity<JwtAuthenticationResponse> {
    if (authenticationFacade.userAccountEntity.isMfaEnabled) {
      mfaService.checkMfa(authenticationFacade.userAccountEntity, req.otp)
    } else {
      if (req.password.isNullOrBlank()) {
        throw AuthenticationException(Message.WRONG_CURRENT_PASSWORD)
      }
      val matches = passwordEncoder.matches(req.password, authenticationFacade.userAccountEntity.password)
      if (!matches) throw AuthenticationException(Message.WRONG_CURRENT_PASSWORD)
    }

    val jwt = jwtTokenProvider.generateToken(authenticationFacade.userAccount.id, true).toString()
    return ResponseEntity.ok(JwtAuthenticationResponse(jwt))
  }
}
