package io.tolgee.controllers.resetPassword

import io.tolgee.component.email.TolgeeEmailSender
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.misc.EmailParams
import io.tolgee.dtos.request.auth.ResetPasswordRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.DisabledFunctionalityException
import io.tolgee.model.UserAccount
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.security.UserAccountService
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.context.ApplicationContext
import java.util.*

class ResetPasswordRequestHandler(
  private val applicationContext: ApplicationContext,
  private val request: ResetPasswordRequest,
) {
  fun handle() {
    if (!authProperties.nativeEnabled) {
      throw DisabledFunctionalityException(Message.NATIVE_AUTHENTICATION_DISABLED)
    }

    val userAccount = userAccount ?: return

    checkUserHasVerifiedEmail(userAccount)

    if (userAccount.accountType === UserAccount.AccountType.MANAGED) {
      sendErrorEmailForNonNativeAuthUser()
      return
    }

    val url = saveSecretCodeAndGetCallbackUrl()
    sendPasswordResetEmail(url)
  }

  private fun sendPasswordResetEmail(url: String) {
    val isInitial = userAccount!!.accountType == UserAccount.AccountType.THIRD_PARTY

    val params =
      EmailParams(
        to = request.email,
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

  private fun sendErrorEmailForNonNativeAuthUser() {
    val params =
      EmailParams(
        to = request.email,
        subject = "Password reset - SSO managed account",
        text =
          """
          Hello! 👋<br/><br/>
          We received a request to reset the password for your account. However, your account is managed by your organization and uses a single sign-on (SSO) service to log in.<br/><br/>
          To access your account, please use the "SSO Login" button on the Tolgee login page. No password reset is needed.<br/><br/>
          If you did not make this request, you may safely ignore this email.<br/><br/>
          
          Regards,<br/>
          Tolgee
          """.trimIndent(),
      )

    tolgeeEmailSender.sendEmail(params)
  }

  private fun checkUserHasVerifiedEmail(userAccount: UserAccount) {
    if (!emailVerificationService.isVerified(userAccount)) {
      throw BadRequestException(Message.EMAIL_NOT_VERIFIED)
    }
  }

  private fun saveSecretCodeAndGetCallbackUrl(): String {
    val code = generateCode()
    saveCode(code)

    val callbackString = code + "," + request.email
    return getCallbackUrlBase() + "/" + Base64.getEncoder().encodeToString(callbackString.toByteArray())
  }

  private fun getCallbackUrlBase(): String? {
    return request.callbackUrl
  }

  private fun saveCode(code: String?) {
    userAccountService.setResetPasswordCode(userAccount!!, code)
  }

  private fun generateCode(): String? = RandomStringUtils.randomAlphabetic(50)

  private val userAccount by lazy {
    userAccountService.findActive(request.email)
  }

  private val authProperties: AuthenticationProperties by lazy {
    applicationContext.getBean(AuthenticationProperties::class.java)
  }

  private val userAccountService: UserAccountService by lazy {
    applicationContext.getBean(UserAccountService::class.java)
  }

  private val emailVerificationService: EmailVerificationService by lazy {
    applicationContext.getBean(EmailVerificationService::class.java)
  }

  private val tolgeeEmailSender: TolgeeEmailSender by lazy {
    applicationContext.getBean(TolgeeEmailSender::class.java)
  }
}
