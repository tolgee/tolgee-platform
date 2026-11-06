package io.tolgee.security.authentication

import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

/** The guest principal satisfies Spring's `authenticated()` rule, so this interceptor keeps that rule's meaning for a guest. */
@Component
class GuestAccessInterceptor(
  @Lazy
  private val authenticationFacade: AuthenticationFacade,
) : HandlerInterceptor {
  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
  ): Boolean {
    if (handler !is HandlerMethod || DispatcherType.ASYNC == request.dispatcherType) {
      return super.preHandle(request, response, handler)
    }
    if (request.method == "OPTIONS") return true
    if (!handler.hasMethodAnnotation(AllowGuestAccess::class.java)) authenticationFacade.requireNotGuest()
    return true
  }
}
