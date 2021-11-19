package io.tolgee.security.api_key_auth

import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.exceptions.PermissionException
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.SecurityService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class ApiKeyAuthFilter(
  private val apiKeyService: io.tolgee.service.ApiKeyService,
  private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  @param:Qualifier("handlerExceptionResolver")
  private val resolver: HandlerExceptionResolver,
) : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    if (this.isApiAccessAllowed(request)) {
      val keyParameter = request.getParameter("ak")
      if (keyParameter != null && !keyParameter.isEmpty()) {
        val ak = apiKeyService.getApiKey(keyParameter)
        if (ak.isPresent) {
          val apiKeyAuthenticationToken = ApiKeyAuthenticationToken(ak.get())
          SecurityContextHolder.getContext().authentication = apiKeyAuthenticationToken

          val apiScopes = this.getAccessAllowedAnnotation(request)!!.scopes
          try {
            val key = ak.get()
            securityService.checkApiKeyScopes(setOf(*apiScopes), key)
            projectHolder.project = ProjectDto.fromEntity(key.project)
          } catch (e: PermissionException) {
            resolver.resolveException(request, response, null, e)
            return
          }
        }
      }
    }
    filterChain.doFilter(request, response)
  }

  private fun isApiAccessAllowed(request: HttpServletRequest): Boolean {
    return this.getAccessAllowedAnnotation(request) != null
  }

  private fun getAccessAllowedAnnotation(request: HttpServletRequest): AccessWithApiKey? {
    return (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
      ?.getMethodAnnotation(AccessWithApiKey::class.java) ?: return null
  }
}
