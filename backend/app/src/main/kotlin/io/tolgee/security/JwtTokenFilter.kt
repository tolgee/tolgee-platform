package io.tolgee.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.exceptions.AuthenticationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver
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
  private val resolver: HandlerExceptionResolver

) : OncePerRequestFilter() {
  @Throws(ServletException::class, IOException::class)
  override fun doFilterInternal(
    req: HttpServletRequest,
    res: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val token = jwtTokenProvider.resolveToken(req)
    try {
      if (token != null && jwtTokenProvider.validateToken(token)) {
        val auth = jwtTokenProvider.getAuthentication(token)
        SecurityContextHolder.getContext().authentication = auth
      }
      filterChain.doFilter(req, res)
    } catch (e: AuthenticationException) {
      resolver.resolveException(req, res, null, e)
    }
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    return !configuration.authentication.enabled
  }
}
