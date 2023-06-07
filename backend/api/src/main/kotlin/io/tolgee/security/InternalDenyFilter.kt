package io.tolgee.security

import io.tolgee.configuration.tolgee.InternalProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class InternalDenyFilter(
  private val internalProperties: InternalProperties,
  private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
  @param:Qualifier("handlerExceptionResolver")
  private val resolver: HandlerExceptionResolver
) : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    try {
      if (isInternal(request) && !isInternalAllowed()) {
        response.status = 403
        response.outputStream.print("Internal access is not allowed")
        return
      }
      filterChain.doFilter(request, response)
    } catch (e: HttpRequestMethodNotSupportedException) {
      resolver.resolveException(request, response, null, e)
    }
  }

  private fun isInternal(request: HttpServletRequest): Boolean {
    return request.requestURI.startsWith("/internal") ||
      (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
      ?.method?.declaringClass
      ?.getAnnotation(InternalController::class.java) != null
  }

  private fun isInternalAllowed() = internalProperties.controllerEnabled
}
