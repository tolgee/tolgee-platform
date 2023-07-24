package io.tolgee.security

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.PermissionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class JwtTokenFilter @Autowired constructor(
  private val jwtTokenProvider: JwtTokenProviderImpl,
  private val configuration: TolgeeProperties,
  @param:Qualifier("handlerExceptionResolver")
  private val resolver: HandlerExceptionResolver,
  private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
  private val currentDateProvider: CurrentDateProvider,
) : OncePerRequestFilter() {
  @Throws(ServletException::class, IOException::class)
  override fun doFilterInternal(
    req: HttpServletRequest,
    res: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val token = jwtTokenProvider.resolveToken(req)
    try {
      if (token != null) {
        val auth = jwtTokenProvider.getAuthentication(token)
        SecurityContextHolder.getContext().authentication = auth
        val userAccountDto = auth.principal as UserAccountDto
        if (userAccountDto.needsSuperJwt) {
          val isSuperTokenValid = token.superExpiration
            ?.let { it >= currentDateProvider.date.time } ?: false
          val needsSuperJwtToken = needsSuperToken(req)
          if (needsSuperJwtToken && !isSuperTokenValid) {
            throw PermissionException(Message.EXPIRED_SUPER_JWT_TOKEN)
          }
        }
      }
      filterChain.doFilter(req, res)
    } catch (e: Exception) {
      resolver.resolveException(req, res, null, e)
    }
  }

  private fun needsSuperToken(request: HttpServletRequest): Boolean {
    return (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
      ?.method?.getAnnotation(NeedsSuperJwtToken::class.java) != null
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    return !configuration.authentication.enabled
  }
}
