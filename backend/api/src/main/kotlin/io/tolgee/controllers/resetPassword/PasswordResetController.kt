package io.tolgee.controllers.resetPassword

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.constants.Message
import io.tolgee.controllers.AuthenticationTag
import io.tolgee.dtos.request.auth.ResetPassword
import io.tolgee.dtos.request.auth.ResetPasswordRequest
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.DisabledFunctionalityException
import io.tolgee.model.UserAccount
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.security.UserAccountService
import jakarta.validation.Valid
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/public")
@AuthenticationTag
class PasswordResetController(
  private val userAccountService: UserAccountService,
  private val applicationContext: ApplicationContext,
  private val authProperties: AuthenticationProperties,
) {
  @Operation(summary = "Request password reset")
  @PostMapping("/reset_password_request")
  @OpenApiHideFromPublicDocs
  @RateLimited(limit = 2, refillDurationInMs = 300000)
  fun resetPasswordRequest(
    @RequestBody @Valid
    request: ResetPasswordRequest,
  ) {
    ResetPasswordRequestHandler(applicationContext, request).handle()
  }

  @GetMapping("/reset_password_validate/{email}/{code}")
  @Operation(summary = "Validate password-resetting key")
  @OpenApiHideFromPublicDocs
  fun resetPasswordValidate(
    @PathVariable("code") code: String,
    @PathVariable("email") email: String,
  ) {
    if (!authProperties.nativeEnabled) {
      throw DisabledFunctionalityException(Message.NATIVE_AUTHENTICATION_DISABLED)
    }
    validateEmailCode(code, email)
  }

  @PostMapping("/reset_password_set")
  @Operation(summary = "Set a new password", description = "Checks the password reset code from e-mail")
  @OpenApiHideFromPublicDocs
  fun resetPasswordSet(
    @RequestBody @Valid
    request: ResetPassword,
  ) {
    if (!authProperties.nativeEnabled) {
      throw DisabledFunctionalityException(Message.NATIVE_AUTHENTICATION_DISABLED)
    }
    val userAccount = validateEmailCode(request.code!!, request.email!!)
    if (userAccount.accountType === UserAccount.AccountType.MANAGED) {
      throw AuthenticationException(Message.OPERATION_UNAVAILABLE_FOR_ACCOUNT_TYPE)
    }
    if (userAccount.accountType === UserAccount.AccountType.THIRD_PARTY) {
      userAccountService.setAccountType(userAccount, UserAccount.AccountType.LOCAL)
    }
    userAccountService.setUserPassword(userAccount, request.password)
    userAccountService.removeResetCode(userAccount)
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
