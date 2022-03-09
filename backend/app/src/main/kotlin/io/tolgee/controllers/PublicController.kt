package io.tolgee.controllers

import com.fasterxml.jackson.databind.node.TextNode
import com.sun.istack.NotNull
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.development.DbPopulatorReal
import io.tolgee.dtos.request.auth.ResetPassword
import io.tolgee.dtos.request.auth.ResetPasswordRequest
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Invitation
import io.tolgee.model.UserAccount
import io.tolgee.security.JwtTokenProviderImpl
import io.tolgee.security.payload.ApiResponse
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.payload.LoginRequest
import io.tolgee.security.third_party.GithubOAuthDelegate
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.InvitationService
import io.tolgee.service.UserAccountService
import io.tolgee.service.security.ReCaptchaValidationService
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/public")
@Tag(name = "Authentication")
class PublicController(
  private val authenticationManager: AuthenticationManager,
  private val tokenProvider: JwtTokenProviderImpl,
  private val githubOAuthDelegate: GithubOAuthDelegate,
  private val properties: TolgeeProperties,
  private val userAccountService: UserAccountService,
  private val mailSender: JavaMailSender,
  private val invitationService: InvitationService,
  private val emailVerificationService: EmailVerificationService,
  private val dbPopulatorReal: DbPopulatorReal,
  private val reCaptchaValidationService: ReCaptchaValidationService,
  private val tolgeeProperties: TolgeeProperties
) {
  @Operation(summary = "Generates JWT token")
  @PostMapping("/generatetoken")
  fun authenticateUser(@RequestBody loginRequest: LoginRequest): ResponseEntity<*> {
    if (loginRequest.username.isEmpty() || loginRequest.password.isEmpty()) {
      return ResponseEntity(
        ApiResponse(false, io.tolgee.constants.Message.USERNAME_OR_PASSWORD_INVALID.code),
        HttpStatus.BAD_REQUEST
      )
    }
    if (properties.authentication.ldap.enabled && properties.authentication.nativeEnabled) {
      // todo: validate properties
      throw RuntimeException("Can not use native auth and ldap auth in the same time")
    }
    var jwt: String? = null
    if (properties.authentication.ldap.enabled) {
      jwt = doLdapAuthorization(loginRequest)
    }
    if (properties.authentication.nativeEnabled) {
      jwt = doNativeAuth(loginRequest)
    }
    if (jwt == null) {
      // todo: validate properties
      throw RuntimeException("Authentication method not configured")
    }
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
    var invitation: Invitation? = null
    if (dto.invitationCode == null) {
      properties.authentication.checkAllowedRegistrations()
    } else {
      invitation = invitationService.getInvitation(dto.invitationCode) // it throws an exception
    }

    userAccountService.findOptional(dto.email).ifPresent {
      throw BadRequestException(io.tolgee.constants.Message.USERNAME_ALREADY_EXISTS)
    }

    if (!reCaptchaValidationService.validate(dto.recaptchaToken, "")) {
      throw BadRequestException(io.tolgee.constants.Message.INVALID_RECAPTCHA_TOKEN)
    }

    val user = userAccountService.createUser(dto)
    if (invitation != null) {
      invitationService.accept(invitation.code, user)
    }

    if (!tolgeeProperties.authentication.needsEmailVerification) {
      return JwtAuthenticationResponse(tokenProvider.generateToken(user.id).toString())
    }

    emailVerificationService.createForUser(user, dto.callbackUrl)
    return null
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

  @GetMapping("/authorize_oauth/{serviceType}/{code}")
  @Operation(summary = "Authenticates user using third party oAuth service")
  fun authenticateUser(
    @PathVariable("serviceType") serviceType: String?,
    @PathVariable("code") code: String?,
    @RequestParam(value = "invitationCode", required = false) invitationCode: String?
  ): JwtAuthenticationResponse {
    if (properties.internal.fakeGithubLogin && code == "this_is_dummy_code") {
      val user = dbPopulatorReal.createUserIfNotExists("johndoe@doe.com")
      return JwtAuthenticationResponse(tokenProvider.generateToken(user.id).toString())
    }
    return githubOAuthDelegate.getTokenResponse(code, invitationCode)
  }

  private fun doNativeAuth(loginRequest: LoginRequest): String {
    val userAccount = userAccountService.findOptional(loginRequest.username).orElseThrow {
      AuthenticationException(io.tolgee.constants.Message.BAD_CREDENTIALS)
    }
    val bCryptPasswordEncoder = BCryptPasswordEncoder()
    val matches = bCryptPasswordEncoder.matches(loginRequest.password, userAccount.password)
    if (!matches) {
      throw AuthenticationException(io.tolgee.constants.Message.BAD_CREDENTIALS)
    }
    emailVerificationService.check(userAccount)
    return tokenProvider.generateToken(userAccount.id).toString()
  }

  private fun doLdapAuthorization(loginRequest: LoginRequest): String {
    return try {
      val authentication = authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken(
          loginRequest.username,
          loginRequest.password
        )
      )
      val userPrincipal = authentication.principal as LdapUserDetailsImpl
      val userAccountEntity = userAccountService.findOptional(userPrincipal.username).orElseGet {
        val userAccount = UserAccount()
        userAccount.username = userPrincipal.username
        userAccountService.createUser(userAccount)
        userAccount
      }
      tokenProvider.generateToken(userAccountEntity.id).toString()
    } catch (e: BadCredentialsException) {
      throw AuthenticationException(io.tolgee.constants.Message.BAD_CREDENTIALS)
    }
  }

  private fun validateEmailCode(code: String, email: String): UserAccount {
    val userAccount = userAccountService.findOptional(email).orElseThrow { NotFoundException() }
      ?: throw BadRequestException(io.tolgee.constants.Message.BAD_CREDENTIALS)
    val resetCodeValid = userAccountService.isResetCodeValid(userAccount, code)
    if (!resetCodeValid) {
      throw BadRequestException(io.tolgee.constants.Message.BAD_CREDENTIALS)
    }
    return userAccount
  }
}
