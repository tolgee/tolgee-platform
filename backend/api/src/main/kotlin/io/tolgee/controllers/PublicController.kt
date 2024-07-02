package io.tolgee.controllers

import com.fasterxml.jackson.databind.node.TextNode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.email.TolgeeEmailSender
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.misc.EmailParams
import io.tolgee.dtos.request.auth.ResetPassword
import io.tolgee.dtos.request.auth.ResetPasswordRequest
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.dtos.security.LoginRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.authorization.BypassEmailVerification
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.security.thirdParty.GithubOAuthDelegate
import io.tolgee.security.thirdParty.GoogleOAuthDelegate
import io.tolgee.security.thirdParty.OAuth2Delegate
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.security.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/public")
@Tag(name = "Authentication")
class PublicController(
  private val jwtService: JwtService,
  private val githubOAuthDelegate: GithubOAuthDelegate,
  private val googleOAuthDelegate: GoogleOAuthDelegate,
  private val oauth2Delegate: OAuth2Delegate,
  private val properties: TolgeeProperties,
  private val userAccountService: UserAccountService,
  private val tolgeeEmailSender: TolgeeEmailSender,
  private val emailVerificationService: EmailVerificationService,
  private val reCaptchaValidationService: ReCaptchaValidationService,
  private val signUpService: SignUpService,
  private val mfaService: MfaService,
  private val userCredentialsService: UserCredentialsService,
) {
  @Operation(summary = "Generate JWT token")
  @PostMapping("/generatetoken")
  @RateLimited(5, isAuthentication = true)
  fun authenticateUser(
    @RequestBody @Valid
    loginRequest: LoginRequest,
  ): JwtAuthenticationResponse {
    val userAccount = userCredentialsService.checkUserCredentials(loginRequest.username, loginRequest.password)
    mfaService.checkMfa(userAccount, loginRequest.otp)

    // two factor passed, so we can generate super token
    val jwt = jwtService.emitToken(userAccount.id, true)
    return JwtAuthenticationResponse(jwt)
  }

  @Operation(summary = "Request password reset")
  @PostMapping("/reset_password_request")
  @OpenApiHideFromPublicDocs
  fun resetPasswordRequest(
    @RequestBody @Valid
    request: ResetPasswordRequest,
  ) {
    val userAccount = userAccountService.findActive(request.email!!) ?: return
    val code = RandomStringUtils.randomAlphabetic(50)
    userAccountService.setResetPasswordCode(userAccount, code)

    val callbackString = code + "," + request.email
    val url = request.callbackUrl + "/" + Base64.getEncoder().encodeToString(callbackString.toByteArray())
    val isInitial = userAccount.accountType == UserAccount.AccountType.THIRD_PARTY

    val params =
      EmailParams(
        to = request.email!!,
        subject = if (isInitial) "Initial password configuration" else "Password reset",
        text =
          """
          Hello! 👋<br/><br/>
          ${if (isInitial) "To set a password for your account, <b>follow this link</b>:<br/>" else "To reset your password, <b>follow this link</b>:<br/>"}
          <a href="$url">$url</a><br/><br/>
          If you have not requested this e-mail, please ignore it.<br/><br/>
          
          Regards,<br/>
          Tolgee
          """.trimIndent(),
      )

    tolgeeEmailSender.sendEmail(params)
  }

  @GetMapping("/reset_password_validate/{email}/{code}")
  @Operation(summary = "Validate password-resetting key")
  @OpenApiHideFromPublicDocs
  fun resetPasswordValidate(
    @PathVariable("code") code: String,
    @PathVariable("email") email: String,
  ) {
    validateEmailCode(code, email)
  }

  @PostMapping("/reset_password_set")
  @Operation(summary = "Set a new password", description = "Checks the password reset code from e-mail")
  @OpenApiHideFromPublicDocs
  fun resetPasswordSet(
    @RequestBody @Valid
    request: ResetPassword,
  ) {
    val userAccount = validateEmailCode(request.code!!, request.email!!)
    if (userAccount.accountType === UserAccount.AccountType.THIRD_PARTY) {
      userAccountService.setAccountType(userAccount, UserAccount.AccountType.LOCAL)
    }
    userAccountService.setUserPassword(userAccount, request.password)
    userAccountService.removeResetCode(userAccount)
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
  ): JwtAuthenticationResponse {
    if (properties.internal.fakeGithubLogin && code == "this_is_dummy_code") {
      val user = getFakeGithubUser()
      return JwtAuthenticationResponse(jwtService.emitToken(user.id))
    }
    return when (serviceType) {
      "github" -> {
        githubOAuthDelegate.getTokenResponse(code, invitationCode)
      }

      "google" -> {
        googleOAuthDelegate.getTokenResponse(code, invitationCode, redirectUri)
      }

      "oauth2" -> {
        oauth2Delegate.getTokenResponse(code, invitationCode, redirectUri)
      }

      else -> {
        throw NotFoundException(Message.SERVICE_NOT_FOUND)
      }
    }
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

  private fun validateEmailCode(
    code: String,
    email: String,
  ): UserAccount {
    val userAccount = userAccountService.findActive(email) ?: throw BadRequestException(Message.BAD_CREDENTIALS)
    val resetCodeValid = userAccountService.isResetCodeValid(userAccount, code)
    if (!resetCodeValid) {
      throw BadRequestException(Message.BAD_CREDENTIALS)
    }
    return userAccount
  }
}
