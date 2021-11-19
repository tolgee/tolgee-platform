package io.tolgee.security.project_auth

import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class ProjectPermissionFilter(
  private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
  private val securityService: SecurityService,
  @param:Qualifier("handlerExceptionResolver")
  private val resolver: HandlerExceptionResolver,
  private val projectHolder: ProjectHolder,
  private val projectService: ProjectService,
  private val authenticationFacade: AuthenticationFacade
) : OncePerRequestFilter() {
  companion object {
    const val REGEX = "/(?:v2|api)/(?:repositor(?:y|ies)|projects?)/([0-9]+)/?.*"
  }

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val matchRegex = REGEX.toRegex()
    if (request.requestURI.matches(matchRegex) && authenticationFacade.authentication?.isAuthenticated == true) {
      val specificPermissionAnnotation = getSpecificPermissionAnnotation(request)
      val anyPermissionAnnotation = getAnyPermissionAnnotation(request)

      if (specificPermissionAnnotation != null && anyPermissionAnnotation != null) {
        throw Exception(
          "Cannot use both AccessWithProjectPermission" +
            " and AccessWithAnyProjectPermission annotations."
        )
      }

      try {
        val projectId = request.projectId
        projectHolder.project = projectService.findDto(projectId) ?: throw NotFoundException()

        if (specificPermissionAnnotation != null) {
          securityService.checkProjectPermission(projectId, specificPermissionAnnotation.permission)
        }

        if (anyPermissionAnnotation != null) {
          securityService.checkProjectPermission(projectId, Permission.ProjectPermissionType.VIEW)
        }
      } catch (e: Exception) {
        resolver.resolveException(request, response, null, e)
        return
      }
    }
    filterChain.doFilter(request, response)
  }

  private val HttpServletRequest.projectId
    get() = this.requestURI
      .replace(REGEX.toRegex(), "$1").toLong()

  private fun getSpecificPermissionAnnotation(request: HttpServletRequest): AccessWithProjectPermission? {
    val handlerMethod = (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
    return handlerMethod?.getMethodAnnotation(AccessWithProjectPermission::class.java)
  }

  private fun getAnyPermissionAnnotation(request: HttpServletRequest): AccessWithAnyProjectPermission? {
    val handlerMethod = (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
    return handlerMethod?.getMethodAnnotation(AccessWithAnyProjectPermission::class.java)
  }
}
