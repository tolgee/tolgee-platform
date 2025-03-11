package io.tolgee.controllers

import com.fasterxml.jackson.databind.node.TextNode
import io.swagger.v3.oas.annotations.Operation
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.dtos.security.LoginRequest
import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.DisabledFunctionalityException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.invitation.PublicInvitationModel
import io.tolgee.hateoas.invitation.PublicInvitationModelAssembler
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.security.service.thirdParty.ThirdPartyAuthDelegate
import io.tolgee.security.thirdParty.ThirdPartyUserHandler
import io.tolgee.security.thirdParty.data.ThirdPartyUserDetails
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.TenantService
import io.tolgee.service.invitation.InvitationService
import io.tolgee.service.security.MfaService
import io.tolgee.service.security.ReCaptchaValidationService
import io.tolgee.service.security.SignUpService
import io.tolgee.service.security.UserAccountService
import io.tolgee.service.security.UserCredentialsService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/public")
@AuthenticationTag
class PublicController(
  private val jwtService: JwtService,
  private val properties: TolgeeProperties,
  private val userAccountService: UserAccountService,
  private val emailVerificationService: EmailVerificationService,
  private val reCaptchaValidationService: ReCaptchaValidationService,
  private val signUpService: SignUpService,
  private val mfaService: MfaService,
  private val userCredentialsService: UserCredentialsService,
  private val authProperties: AuthenticationProperties,
  private val thirdPartyAuthDelegates: List<ThirdPartyAuthDelegate>,
  private val thirdPartyUserHandler: ThirdPartyUserHandler,
  private val tenantService: TenantService,
  private val publicInvitationModelAssembler: PublicInvitationModelAssembler,
  private val invitationService: InvitationService,
) {
  @Operation(summary = "Generate JWT token")
  @PostMapping("/generatetoken")
  @RateLimited(5, isAuthentication = true)
  fun authenticateUser(
    @RequestBody @Valid
    loginRequest: LoginRequest,
  ): JwtAuthenticationResponse {
    if (!authProperties.nativeEnabled) {
      throw AuthenticationException(Message.NATIVE_AUTHENTICATION_DISABLED)
    }

    val userAccount = userCredentialsService.checkUserCredentials(loginRequest.username, loginRequest.password)
    mfaService.checkMfa(userAccount, loginRequest.otp)

    // two factor passed, so we can generate super token
    val jwt = jwtService.emitToken(userAccount.id, true)
    return JwtAuthenticationResponse(jwt)
  }

  @PostMapping("/sign_up")
  @Transactional
  @OpenApiHideFromPublicDocs
  @Operation(
    summary = "Create new user account (Sign Up)",
    description = "When E-mail verification is enabled, null is returned. Otherwise JWT token is provided.",
  )
  fun signUp(
    @RequestBody @Valid
    dto: SignUpDto,
  ): JwtAuthenticationResponse? {
    if (!reCaptchaValidationService.validate(dto.recaptchaToken, "")) {
      throw BadRequestException(Message.INVALID_RECAPTCHA_TOKEN)
    }
    if (!authProperties.nativeEnabled) {
      throw DisabledFunctionalityException(Message.NATIVE_AUTHENTICATION_DISABLED)
    }
    return signUpService.signUp(dto)
  }

  @GetMapping("/verify_email/{userId}/{code}")
  @Operation(
    summary = "Set user account as verified",
    description = "It checks whether the code from email is valid",
  )
  @OpenApiHideFromPublicDocs
  fun verifyEmail(
    @PathVariable("userId") @NotNull userId: Long,
    @PathVariable("code") @NotBlank code: String,
  ): JwtAuthenticationResponse {
    emailVerificationService.verify(userId, code)
    return JwtAuthenticationResponse(jwtService.emitToken(userId))
  }

  @PostMapping(value = ["/validate_email"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(summary = "Validate if email is not in use")
  @OpenApiHideFromPublicDocs
  fun validateEmail(
    @RequestBody email: TextNode,
  ): Boolean {
    return userAccountService.findActive(email.asText()) == null
  }

  @GetMapping("/authorize_oauth/{serviceType}")
  @Operation(
    summary = "Authenticate user (third-part, oAuth)",
    description = "Authenticates user using third party oAuth service",
  )
  @Transactional
  fun authenticateUser(
    @PathVariable("serviceType") serviceType: String?,
    @RequestParam(value = "code", required = true) code: String?,
    @RequestParam(value = "redirect_uri", required = true) redirectUri: String?,
    @RequestParam(value = "invitationCode", required = false) invitationCode: String?,
    @RequestParam(value = "domain", required = false) domain: String?,
  ): JwtAuthenticationResponse {
    val delegate =
      thirdPartyAuthDelegates.find { it.name == serviceType }
        ?: throw NotFoundException(Message.SERVICE_NOT_FOUND)

    if (properties.internal.fakeThirdPartyLogin && code?.startsWith(DUMMY_CODE_PREFIX) == true) {
      val username = code.removePrefix(DUMMY_CODE_PREFIX)
      val data =
        DummyThirdPartyUserDetails(
          username = username,
          name = username,
          thirdPartyAuthType = delegate.preferredAuthType,
        )
      return fakeThirdPartyLogin(data, invitationCode, domain)
    }

    return delegate.getTokenResponse(code, invitationCode, redirectUri, domain)
  }

  @GetMapping("/invitation_info/{code}")
  @Operation(summary = "Info about invitation")
  fun invitationInfo(
    @PathVariable("code") code: String?,
  ): PublicInvitationModel {
    val invitation = invitationService.getInvitation(code)
    return publicInvitationModelAssembler.toModel(invitation)
  }

  private fun fakeThirdPartyLogin(
    data: DummyThirdPartyUserDetails,
    invitationCode: String?,
    domain: String?,
  ): JwtAuthenticationResponse {
    var tenant: SsoTenantConfig? = null
    if (data.thirdPartyAuthType == ThirdPartyAuthType.SSO) {
      tenant = tenantService.getEnabledConfigByDomain(domain)
      if (tenant.global) {
        // We have found global tenant - fix the auth type accordingly
        data.thirdPartyAuthType = ThirdPartyAuthType.SSO_GLOBAL
      }
    }
    val user =
      thirdPartyUserHandler.findOrCreateUser(
        ThirdPartyUserDetails(
          authId = "dummy_auth_id",
          username = data.username,
          name = data.name,
          thirdPartyAuthType = data.thirdPartyAuthType,
          invitationCode = invitationCode,
          refreshToken = null,
          tenant = tenant,
        ),
      )
    val jwt = jwtService.emitToken(user.id)
    return JwtAuthenticationResponse(jwt)
  }

  data class DummyThirdPartyUserDetails(
    var username: String,
    var name: String,
    var thirdPartyAuthType: ThirdPartyAuthType,
  )

  companion object {
    const val DUMMY_CODE_PREFIX = "this_is_dummy_code_"
  }
}
