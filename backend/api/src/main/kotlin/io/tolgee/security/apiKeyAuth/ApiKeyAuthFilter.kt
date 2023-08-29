package io.tolgee.security.apiKeyAuth

import io.tolgee.API_KEY_HEADER_NAME
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.exceptions.PermissionException
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.SecurityService
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
  private val apiKeyService: ApiKeyService,
  private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  @param:Qualifier("handlerExceptionResolver")
  private val resolver: HandlerExceptionResolver,
  private val currentDateProvider: CurrentDateProvider
) : OncePerRequestFilter() {
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    if (this.isApiAccessAllowed(request)) {
      val rawApiKey = getApiKey(request)

      if (!rawApiKey.isNullOrEmpty()) {
        val apiKeyEntity = apiKeyService.find(apiKeyService.hashKey(rawApiKey))
        val urlProjectId = getRequestProjectId(request)
        val isAuthorizedForProject = apiKeyEntity?.project?.id == urlProjectId || urlProjectId == null
        if (apiKeyEntity != null && isAuthorizedForProject) {
          val isExpired = apiKeyEntity.expiresAt?.let { it < currentDateProvider.date } ?: false
          if (!isExpired) {
            val apiKeyAuthenticationToken = ApiKeyAuthenticationToken(apiKeyEntity)
            SecurityContextHolder.getContext().authentication = apiKeyAuthenticationToken

            val scopes = this.getProjectPermissionAnnotation(request)?.scope?.let { setOf(it) }
              ?: emptySet()

            try {
              securityService.fixInvalidApiKeyWhenRequired(apiKeyEntity)
              securityService.checkApiKeyScopes(scopes, apiKeyEntity)
              projectHolder.project = ProjectDto.fromEntity(apiKeyEntity.project)
              apiKeyService.updateLastUsedAsync(apiKeyEntity)
            } catch (e: PermissionException) {
              resolver.resolveException(request, response, null, e)
              return
            }
          }
        }
      }
    }

    filterChain.doFilter(request, response)
  }

  private fun getApiKey(request: HttpServletRequest): String? {
    val rawWithPossiblePrefix = request.getHeader(API_KEY_HEADER_NAME)
      ?: request.getParameter("ak")

    return apiKeyService.parseApiKey(rawWithPossiblePrefix)
  }

  private fun isApiAccessAllowed(request: HttpServletRequest): Boolean {
    return this.getAccessAllowedAnnotation(request) != null
  }

  private fun getAccessAllowedAnnotation(request: HttpServletRequest): AccessWithApiKey? {
    return getHandlerMethodAnnotation(request, AccessWithApiKey::class.java)
  }

  private fun getProjectPermissionAnnotation(request: HttpServletRequest): AccessWithProjectPermission? {
    return getHandlerMethodAnnotation(request, AccessWithProjectPermission::class.java)
  }

  private fun <T : Annotation> getHandlerMethodAnnotation(request: HttpServletRequest, annotation: Class<T>) =
    (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
      ?.getMethodAnnotation(annotation)

  private fun getRequestProjectId(request: HttpServletRequest): Long? {
    val requestURI = request.requestURI
    return "/projects?/([0-9]+)".toRegex().find(requestURI)?.groupValues?.getOrNull(1)?.toLong()
  }
}
