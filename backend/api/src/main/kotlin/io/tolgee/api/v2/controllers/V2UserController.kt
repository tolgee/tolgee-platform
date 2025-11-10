package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.ActivityHolder
import io.tolgee.api.isMfaEnabled
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.request.SuperTokenRequest
import io.tolgee.dtos.request.UserUpdatePasswordRequestDto
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.hateoas.organization.PrivateOrganizationModel
import io.tolgee.hateoas.organization.PrivateOrganizationModelAssembler
import io.tolgee.hateoas.organization.SimpleOrganizationModel
import io.tolgee.hateoas.organization.SimpleOrganizationModelAssembler
import io.tolgee.hateoas.sso.PublicSsoTenantModel
import io.tolgee.hateoas.sso.PublicSsoTenantModelAssembler
import io.tolgee.hateoas.userAccount.PrivateUserAccountModel
import io.tolgee.hateoas.userAccount.PrivateUserAccountModelAssembler
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.ImageUploadService
import io.tolgee.service.TenantService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.MfaService
import io.tolgee.service.security.UserAccountService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.hateoas.CollectionModel
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
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

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@RequestMapping("/v2/user")
@Tag(name = "User", description = "Manipulates currently authenticated user")
class V2UserController(
  private val authenticationFacade: AuthenticationFacade,
  private val userAccountService: UserAccountService,
  private val privateUserAccountModelAssembler: PrivateUserAccountModelAssembler,
  private val publicSsoTenantModelAssembler: PublicSsoTenantModelAssembler,
  private val imageUploadService: ImageUploadService,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
  private val privateOrganizationModelAssembler: PrivateOrganizationModelAssembler,
  private val tenantService: TenantService,
  private val simpleOrganizationModelAssembler: SimpleOrganizationModelAssembler,
  private val passwordEncoder: PasswordEncoder,
  private val jwtService: JwtService,
  private val mfaService: MfaService,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val emailVerificationService: EmailVerificationService,
  @Qualifier("requestActivityHolder") private val request: ActivityHolder,
) {
  @Operation(
    summary = "Resend email verification",
    description = "Resends email verification email to currently authenticated user.",
  )
  @PostMapping("/send-email-verification")
  @BypassEmailVerification
  fun sendEmailVerification(request: HttpServletRequest) {
    val user = authenticationFacade.authenticatedUserEntity
    emailVerificationService.resendEmailVerification(user, request)
  }

  @Operation(
    summary = "Get user info",
    description = "Returns information about currently authenticated user.",
  )
  @GetMapping("")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  @AllowApiAccess
  @OpenApiOrderExtension(1)
  fun getInfo(): PrivateUserAccountModel {
    val userAccount = authenticationFacade.authenticatedUserView
    return privateUserAccountModelAssembler.toModel(userAccount)
  }

  @PutMapping("")
  @Operation(summary = "Update user", description = "Updates current user's profile information.")
  @OpenApiOrderExtension(2)
  fun updateUser(
    @RequestBody @Valid
    dto: UserUpdateRequestDto?,
    request: HttpServletRequest,
  ): PrivateUserAccountModel {
    val userAccount = userAccountService.update(authenticationFacade.authenticatedUserEntity, dto!!, request)
    val view =
      userAccountService.findActiveView(userAccount.id)
        ?: throw IllegalStateException("User not found")
    return privateUserAccountModelAssembler.toModel(view)
  }

  @PutMapping("/password")
  @Operation(
    summary = "Update password",
    description = "Updates current user's password. Invalidates all previous sessions upon success.",
  )
  @OpenApiOrderExtension(3)
  fun updateUserPassword(
    @RequestBody @Valid
    dto: UserUpdatePasswordRequestDto?,
  ): JwtAuthenticationResponse {
    userAccountService.updatePassword(authenticationFacade.authenticatedUserEntity, dto!!)
    return JwtAuthenticationResponse(
      jwtService.emitTokenRefreshForCurrentUser(),
    )
  }

  @PutMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Upload avatar")
  @OpenApiOrderExtension(4)
  @ResponseStatus(HttpStatus.OK)
  fun uploadAvatar(
    @RequestParam("avatar") avatar: MultipartFile,
  ): PrivateUserAccountModel {
    imageUploadService.validateIsImage(avatar)
    val entity = authenticationFacade.authenticatedUserEntity
    userAccountService.setAvatar(entity, avatar.inputStream)
    val view =
      userAccountService.findActiveView(entity.id)
        ?: throw IllegalStateException("User not found")
    return privateUserAccountModelAssembler.toModel(view)
  }

  @DeleteMapping("/avatar")
  @Operation(summary = "Delete avatar")
  @ResponseStatus(HttpStatus.OK)
  @OpenApiOrderExtension(5)
  fun removeAvatar(): PrivateUserAccountModel {
    val entity = authenticationFacade.authenticatedUserEntity
    userAccountService.removeAvatar(entity)
    val view =
      userAccountService.findActiveView(entity.id)
        ?: throw IllegalStateException("User not found")
    return privateUserAccountModelAssembler.toModel(view)
  }

  @Operation(summary = "Delete user")
  @DeleteMapping("")
  @RequiresSuperAuthentication
  @OpenApiOrderExtension(6)
  fun delete() {
    userAccountService.delete(authenticationFacade.authenticatedUserEntity)
  }

  @Operation(
    summary = "Get information about SSO configuration",
    description = "Returns information about sso configuration affecting the user.",
  )
  @GetMapping("/sso")
  @ApiResponse(
    responseCode = "200",
    content = [
      Content(
        mediaType = "application/json",
        schema =
          Schema(
            implementation = PublicSsoTenantModel::class,
          ),
      ),
    ],
  )
  @ApiResponse(
    responseCode = "204",
    description = "No SSO configuration available for this user",
    content = [
      Content(
        schema =
          Schema(
            implementation = Void::class,
          ),
      ),
    ],
  )
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  @AllowApiAccess
  fun getSso(): ResponseEntity<PublicSsoTenantModel> {
    val userAccount = authenticationFacade.authenticatedUser
    val domain = userAccount.domain ?: return ResponseEntity.noContent().build()
    val tenant = tenantService.getEnabledConfigByDomainOrNull(domain) ?: return ResponseEntity.noContent().build()
    return ResponseEntity.ok(publicSsoTenantModelAssembler.toModel(tenant))
  }

  @GetMapping("/managed-by")
  @Operation(
    summary = "Get organization which manages user",
    description = "Returns the organization that manages a given user or null",
  )
  @ApiResponse(
    responseCode = "200",
    content = [
      Content(
        mediaType = "application/json",
        schema =
          Schema(
            implementation = PrivateOrganizationModel::class,
          ),
      ),
    ],
  )
  @ApiResponse(
    responseCode = "204",
    description = "No SSO configuration available for this user",
    content = [
      Content(
        schema =
          Schema(
            implementation = Void::class,
          ),
      ),
    ],
  )
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  @OpenApiHideFromPublicDocs
  fun getManagedBy(): ResponseEntity<PrivateOrganizationModel> {
    val userAccount = authenticationFacade.authenticatedUser
    val org = organizationRoleService.getManagedBy(userId = userAccount.id) ?: return ResponseEntity.noContent().build()
    val view =
      organizationService.findPrivateView(org.id, authenticationFacade.authenticatedUser.id)
        ?: return ResponseEntity.noContent().build()
    return ResponseEntity.ok(
      privateOrganizationModelAssembler.toModel(
        view,
        enabledFeaturesProvider.get(view.organization.id),
      ),
    )
  }

  @PostMapping("")
  @Operation(summary = "Updates current user's data.", deprecated = true)
  @OpenApiHideFromPublicDocs
  fun updateUserOld(
    @RequestBody @Valid
    dto: UserUpdateRequestDto?,
    request: HttpServletRequest,
  ): PrivateUserAccountModel = updateUser(dto, request)

  @GetMapping("/single-owned-organizations")
  @Operation(
    summary = "Get all single owned organizations",
    description = "Returns all organizations owned only by current user",
  )
  @ResponseStatus(HttpStatus.OK)
  fun getAllSingleOwnedOrganizations(): CollectionModel<SimpleOrganizationModel> {
    val organizations = organizationService.getAllSingleOwnedByUser(authenticationFacade.authenticatedUserEntity)
    return simpleOrganizationModelAssembler.toCollectionModel(organizations)
  }

  @PostMapping("/generate-super-token")
  @Operation(summary = "Get super JWT", description = "Generates new JWT token permitted to sensitive operations")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun getSuperToken(
    @RequestBody @Valid
    req: SuperTokenRequest,
  ): ResponseEntity<JwtAuthenticationResponse> {
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

    val jwt = jwtService.emitTokenRefreshForCurrentUser(isSuper = true)
    return ResponseEntity.ok(JwtAuthenticationResponse(jwt))
  }
}
