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
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.authentication.RequiresSuperAuthentication
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
  private val jwtService: JwtService,
  private val mfaService: MfaService,
) {
  @Operation(summary = "Returns current user's data.")
  @GetMapping("")
  @AllowApiAccess
  fun getInfo(): PrivateUserAccountModel {
    val userAccount = authenticationFacade.authenticatedUserEntity
    return privateUserAccountModelAssembler.toModel(userAccount)
  }

  @Operation(summary = "Deletes current user.")
  @DeleteMapping("")
  @RequiresSuperAuthentication
  fun delete() {
    userAccountService.delete(authenticationFacade.authenticatedUserEntity)
  }

  @PostMapping("")
  @Operation(summary = "Updates current user's data.", deprecated = true)
  fun updateUserOld(@RequestBody @Valid dto: UserUpdateRequestDto?): PrivateUserAccountModel = updateUser(dto)

  @PutMapping("")
  @Operation(summary = "Updates current user's data.")
  fun updateUser(@RequestBody @Valid dto: UserUpdateRequestDto?): PrivateUserAccountModel {
    val userAccount = userAccountService.update(authenticationFacade.authenticatedUserEntity, dto!!)
    return privateUserAccountModelAssembler.toModel(userAccount)
  }

  @PutMapping("/password")
  @Operation(summary = "Updates current user's password. Invalidates all previous sessions upon success.")
  fun updateUserPassword(@RequestBody @Valid dto: UserUpdatePasswordRequestDto?): JwtAuthenticationResponse {
    userAccountService.updatePassword(authenticationFacade.authenticatedUserEntity, dto!!)
    return JwtAuthenticationResponse(
      jwtService.emitToken(authenticationFacade.authenticatedUser.id, true)
    )
  }

  @PutMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Uploads user's avatar.")
  @ResponseStatus(HttpStatus.OK)
  fun uploadAvatar(
    @RequestParam("avatar") avatar: MultipartFile,
  ): PrivateUserAccountModel {
    imageUploadService.validateIsImage(avatar)
    val entity = authenticationFacade.authenticatedUserEntity
    userAccountService.setAvatar(entity, avatar.inputStream)
    return privateUserAccountModelAssembler.toModel(entity)
  }

  @DeleteMapping("/avatar")
  @Operation(summary = "Deletes user's avatar.")
  @ResponseStatus(HttpStatus.OK)
  fun removeAvatar(): PrivateUserAccountModel {
    val entity = authenticationFacade.authenticatedUserEntity
    userAccountService.removeAvatar(entity)
    return privateUserAccountModelAssembler.toModel(entity)
  }

  @GetMapping("/single-owned-organizations")
  @Operation(summary = "Returns all organizations owned only by current user")
  @ResponseStatus(HttpStatus.OK)
  fun getAllSingleOwnedOrganizations(): CollectionModel<SimpleOrganizationModel> {
    val organizations = organizationService.getAllSingleOwnedByUser(authenticationFacade.authenticatedUserEntity)
    return simpleOrganizationModelAssembler.toCollectionModel(organizations)
  }

  @PostMapping("/generate-super-token")
  @Operation(summary = "Generates new JWT token permitted to sensitive operations")
  fun getSuperToken(@RequestBody @Valid req: SuperTokenRequest): ResponseEntity<JwtAuthenticationResponse> {
    val entity = authenticationFacade.authenticatedUserEntity
    if (entity.isMfaEnabled) {
      mfaService.checkMfa(entity, req.otp)
    } else {
      if (req.password.isNullOrBlank()) {
        throw AuthenticationException(Message.WRONG_CURRENT_PASSWORD)
      }
      val matches = passwordEncoder.matches(req.password, entity.password)
      if (!matches) throw AuthenticationException(Message.WRONG_CURRENT_PASSWORD)
    }

    val jwt = jwtService.emitToken(entity.id, true)
    return ResponseEntity.ok(JwtAuthenticationResponse(jwt))
  }
}
