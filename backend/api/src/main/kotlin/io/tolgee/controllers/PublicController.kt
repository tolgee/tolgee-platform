package io.tolgee.controllers

import com.fasterxml.jackson.databind.node.TextNode
import io.swagger.v3.oas.annotations.Operation
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.dtos.response.AuthProviderDto
import io.tolgee.dtos.security.LoginRequest
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.DisabledFunctionalityException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.invitation.PublicInvitationModel
import io.tolgee.hateoas.invitation.PublicInvitationModelAssembler
import io.tolgee.model.UserAccount
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.authorization.BypassEmailVerification
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.security.service.thirdParty.ThirdPartyAuthDelegate
import io.tolgee.service.EmailVerificationService
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
import java.util.*

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
  @BypassEmailVerification
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
    if (properties.internal.fakeGithubLogin && code == "this_is_dummy_code") {
      val user = getFakeGithubUser()
      return JwtAuthenticationResponse(jwtService.emitToken(user.id))
    }

    return thirdPartyAuthDelegates.find { it.name == serviceType }
      ?.getTokenResponse(code, invitationCode, redirectUri, domain)
      ?: throw NotFoundException(Message.SERVICE_NOT_FOUND)
  }

  @GetMapping("/invitation_info/{code}")
  @Operation(summary = "Info about invitation")
  fun invitationInfo(
    @PathVariable("code") code: String?,
  ): PublicInvitationModel {
    val invitation = invitationService.getInvitation(code)
    return publicInvitationModelAssembler.toModel(invitation)
  }

  @PostMapping("/auth_provider/accept_change")
  @Operation(summary = "Accept change of the third party authentication provider")
  fun acceptChangeAuthProvider() {
//    TODO
  }

  @PostMapping("/auth_provider/reject_change")
  @Operation(summary = "Reject change of the third party authentication provider")
  fun rejectChangeAuthProvider() {
//    TODO
  }

  @GetMapping("/auth_provider/current")
  @Operation(summary = "Get current third party authentication provider")
  fun getCurrentAuthProvider(): AuthProviderDto? {
//    TODO
    return null
  }

  @GetMapping("/auth_provider/changed")
  @Operation(summary = "Get info about authentication provider which can replace the current one")
  fun getChangedAuthProvider(): AuthProviderDto? {
    // TODO
    return null
  }

  private fun getFakeGithubUser(): UserAccount {
    val username = "johndoe@doe.com"
    val user =
      userAccountService.findActive(username) ?: let {
        UserAccount().apply {
          this.username = username
          name = "john"
          accountType = UserAccount.AccountType.THIRD_PARTY
          userAccountService.save(this)
        }
      }
    return user
  }
}
