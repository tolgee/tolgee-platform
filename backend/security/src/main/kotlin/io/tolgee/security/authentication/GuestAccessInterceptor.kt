package io.tolgee.security.authentication

import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Lazy
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

/** The guest principal satisfies Spring's `authenticated()` rule, so this interceptor keeps that rule's meaning for a guest. */
@Component
class GuestAccessInterceptor(
  @Lazy
  private val authenticationFacade: AuthenticationFacade,
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
    if (request.method == "OPTIONS") return true
    if (!authenticationFacade.isGuest) return true
    if (handler.hasMethodAnnotation(AllowGuestAccess::class.java)) return true
    throw AuthenticationException(Message.UNAUTHENTICATED)
  }

  override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}
