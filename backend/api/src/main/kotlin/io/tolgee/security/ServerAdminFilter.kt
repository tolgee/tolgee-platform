package io.tolgee.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.service.security.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class ServerAdminFilter @Autowired constructor(
  private val securityService: SecurityService,
  private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
  private val configuration: TolgeeProperties
) : OncePerRequestFilter() {

  @Throws(ServletException::class, IOException::class)
  override fun doFilterInternal(
    req: HttpServletRequest,
    res: HttpServletResponse,
    filterChain: FilterChain
  ) {
    if (this.needsToBeServerAdmin(req)) {
      securityService.checkUserIsServerAdmin()
    }
    filterChain.doFilter(req, res)
  }

  private fun needsToBeServerAdmin(request: HttpServletRequest): Boolean {
    return (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
      ?.method?.getAnnotation(AccessWithServerAdminPermission::class.java) != null
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    return !configuration.authentication.enabled
  }
}
