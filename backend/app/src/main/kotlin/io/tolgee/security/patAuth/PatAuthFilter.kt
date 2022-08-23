package io.tolgee.security.patAuth

import io.tolgee.API_KEY_HEADER_NAME
import io.tolgee.component.CurrentDateProvider
import io.tolgee.security.PAT_PREFIX
import io.tolgee.service.PatService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class PatAuthFilter(
  private val patService: PatService,
  private val currentDateProvider: CurrentDateProvider,
  private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
) : OncePerRequestFilter() {
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    if (!isAccessDenied(request)) {
      val patString = getPat(request)

      if (!patString.isNullOrEmpty()) {
        val hash = patService.hashToken(patString)
        val patEntity = patService.find(hash)
        val isActive = patEntity?.expiresAt?.let { it > currentDateProvider.date } ?: false
        if (isActive && patEntity != null) {
          val patAuthenticationToken = PatAuthenticationToken(patEntity)
          SecurityContextHolder.getContext().authentication = patAuthenticationToken
          patService.updateLastUsedAsync(patEntity)
        }
      }
    }
    filterChain.doFilter(request, response)
  }

  private fun getPat(request: HttpServletRequest): String? {
    val authorizationHeader = request.getHeader(API_KEY_HEADER_NAME) ?: return null
    val match = "$PAT_PREFIX([0-9A-Za-z]+)".toRegex().find(authorizationHeader)
    return match?.groupValues?.getOrNull(1)
  }

  private fun isAccessDenied(request: HttpServletRequest): Boolean {
    return (requestMappingHandlerMapping.getHandler(request)?.handler as? HandlerMethod?)
      ?.getMethodAnnotation(DenyPatAccess::class.java) != null
  }
}
