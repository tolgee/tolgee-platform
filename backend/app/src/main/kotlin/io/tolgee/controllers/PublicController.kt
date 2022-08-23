package io.tolgee.controllers

import com.fasterxml.jackson.databind.node.TextNode
import com.sun.istack.NotNull
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.development.DbPopulatorReal
import io.tolgee.dtos.request.auth.ResetPassword
import io.tolgee.dtos.request.auth.ResetPasswordRequest
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.security.JwtTokenProviderImpl
import io.tolgee.security.payload.ApiResponse
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.payload.LoginRequest
import io.tolgee.security.third_party.GithubOAuthDelegate
import io.tolgee.security.third_party.GoogleOAuthDelegate
import io.tolgee.security.third_party.OAuth2Delegate
import io.tolgee.service.*
import io.tolgee.service.security.ReCaptchaValidationService
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/public")
@Tag(name = "Authentication")
class PublicController(
  private val tokenProvider: JwtTokenProviderImpl,
  private val githubOAuthDelegate: GithubOAuthDelegate,
  private val googleOAuthDelegate: GoogleOAuthDelegate,
  private val oauth2Delegate: OAuth2Delegate,
  private val properties: TolgeeProperties,
  private val userAccountService: UserAccountService,
  private val mailSender: JavaMailSender,
  private val emailVerificationService: EmailVerificationService,
  private val dbPopulatorReal: DbPopulatorReal,
  private val reCaptchaValidationService: ReCaptchaValidationService,
  private val signUpService: SignUpService,
  private val mfaService: MfaService,
  private val securityService: SecurityService,
  private val userCredentialsService: UserCredentialsService
) {
  @Operation(summary = "Generates JWT token")
  @PostMapping("/generatetoken")
  fun authenticateUser(@RequestBody loginRequest: LoginRequest): ResponseEntity<*> {
    if (loginRequest.username.isEmpty() || loginRequest.password.isEmpty()) {
      return ResponseEntity(
        ApiResponse(false, Message.USERNAME_OR_PASSWORD_INVALID.code),
        HttpStatus.BAD_REQUEST
      )
    }
    if (properties.authentication.ldap.enabled && properties.authentication.nativeEnabled) {
      // todo: validate properties
      throw RuntimeException("Can not use native auth and ldap auth in the same time")
    }
    if (!properties.authentication.ldap.enabled && !properties.authentication.nativeEnabled) {
      // todo: validate properties
      throw RuntimeException("Authentication method not configured")
    }

    val userAccount = userCredentialsService.checkUserCredentials(loginRequest.username, loginRequest.password)
    emailVerificationService.check(userAccount)

    // xxx: this needs to be improved
    if (userAccount.totpKey?.isNotEmpty() == true) {
      if (loginRequest.otp?.isEmpty() != false) {
        throw AuthenticationException(Message.MFA_ENABLED)
      }

      if (!mfaService.validateTotpCode(userAccount, loginRequest.otp)) {
        throw AuthenticationException(Message.INVALID_OTP_CODE)
      }
    }

    val jwt = tokenProvider.generateToken(userAccount.id).toString()
    return ResponseEntity.ok(JwtAuthenticationResponse(jwt))
  }

  @Operation(summary = "Reset password request")
  @PostMapping("/reset_password_request")
  fun resetPasswordRequest(@RequestBody @Valid request: ResetPasswordRequest) {
    val userAccount = userAccountService.findOptional(request.email).orElse(null) ?: return
    val code = RandomStringUtils.randomAlphabetic(50)
    userAccountService.setResetPasswordCode(userAccount, code)
    val message = SimpleMailMessage()
    message.setTo(request.email!!)
    message.subject = "Password reset"
    val callbackString = code + "," + request.email
    val url = request.callbackUrl + "/" + Base64.getEncoder().encodeToString(callbackString.toByteArray())
    message.text = """Hello!
 To reset your password click this link: 
$url

 If you have not requested password reset, please just ignore this e-mail."""
    message.from = properties.smtp.from
    mailSender.send(message)
  }

  @GetMapping("/reset_password_validate/{email}/{code}")
  @Operation(summary = "Validates key sent by email")
  fun resetPasswordValidate(
    @PathVariable("code") code: String,
    @PathVariable("email") email: String
  ) {
    validateEmailCode(code, email)
  }

  @PostMapping("/reset_password_set")
  @Operation(summary = "Sets new password with password reset code from e-mail")
  fun resetPasswordSet(@RequestBody @Valid request: ResetPassword) {
    val userAccount = validateEmailCode(request.code!!, request.email!!)
    userAccountService.setUserPassword(userAccount, request.password)
    userAccountService.removeResetCode(userAccount)
  }

  @PostMapping("/sign_up")
  @Transactional
  @Operation(
    summary = """
Creates new user account.

When E-mail verification is enabled, null is returned. Otherwise JWT token is provided.
    """
  )
  fun signUp(@RequestBody @Valid dto: SignUpDto): JwtAuthenticationResponse? {
    if (!reCaptchaValidationService.validate(dto.recaptchaToken, "")) {
      throw BadRequestException(Message.INVALID_RECAPTCHA_TOKEN)
    }
    return signUpService.signUp(dto)
  }

  @GetMapping("/verify_email/{userId}/{code}")
  @Operation(summary = "Sets user account as verified, when code from email is OK")
  fun verifyEmail(
    @PathVariable("userId") @NotNull userId: Long,
    @PathVariable("code") @NotBlank code: String
  ): JwtAuthenticationResponse {
    emailVerificationService.verify(userId, code)
    return JwtAuthenticationResponse(tokenProvider.generateToken(userId).toString())
  }

  @PostMapping(value = ["/validate_email"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(summary = "Validates if email is not in use")
  fun validateEmail(@RequestBody email: TextNode): Boolean {
    return userAccountService.findOptional(email.asText()).isEmpty
  }

  @GetMapping("/authorize_oauth/{serviceType}")
  @Operation(summary = "Authenticates user using third party oAuth service")
  @Transactional
  fun authenticateUser(
    @PathVariable("serviceType") serviceType: String?,
    @RequestParam(value = "code", required = true) code: String?,
    @RequestParam(value = "redirect_uri", required = true) redirectUri: String?,
    @RequestParam(value = "invitationCode", required = false) invitationCode: String?
  ): JwtAuthenticationResponse {
    if (properties.internal.fakeGithubLogin && code == "this_is_dummy_code") {
      val user = dbPopulatorReal.createUserIfNotExists("johndoe@doe.com")
      return JwtAuthenticationResponse(tokenProvider.generateToken(user.id).toString())
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

  private fun validateEmailCode(code: String, email: String): UserAccount {
    val userAccount = userAccountService.findOptional(email).orElseThrow { NotFoundException() }
      ?: throw BadRequestException(Message.BAD_CREDENTIALS)
    val resetCodeValid = userAccountService.isResetCodeValid(userAccount, code)
    if (!resetCodeValid) {
      throw BadRequestException(Message.BAD_CREDENTIALS)
    }
    return userAccount
  }
}
