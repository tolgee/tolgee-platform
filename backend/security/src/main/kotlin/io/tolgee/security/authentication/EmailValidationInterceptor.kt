package io.tolgee.security.authentication

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.PermissionException
import io.tolgee.service.EmailVerificationService
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Lazy
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class EmailValidationInterceptor(
  private val authenticationFacade: AuthenticationFacade,
  @Lazy
  private val emailVerificationService: EmailVerificationService,
) : HandlerInterceptor,
  Ordered {
  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
  ): Boolean {
    if (handler !is HandlerMethod || DispatcherType.ASYNC == request.dispatcherType) {
      return super.preHandle(request, response, handler)
    }

    if (request.method == "OPTIONS") {
      // Do not process OPTIONS requests
      return true
    }

    if (!authenticationFacade.isAuthenticated) {
      // Allow unauthenticated requests - not our concern
      return super.preHandle(request, response, handler)
    }

    val user = authenticationFacade.authenticatedUser
    checkEmailVerificationOrThrow(user, handler)

    return true
  }

  override fun getOrder(): Int {
    return Ordered.HIGHEST_PRECEDENCE
  }

  private fun checkEmailVerificationOrThrow(
    userAccount: UserAccountDto,
    handler: HandlerMethod,
  ) {
    if (handler.hasMethodAnnotation(BypassEmailVerification::class.java)) {
      return
    }

    if (emailVerificationService.isVerified(userAccount)) {
      return
    }

    throw PermissionException(Message.EMAIL_NOT_VERIFIED)
  }
}
