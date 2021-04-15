package io.tolgee.security.repository_auth

import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.service.RepositoryService
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
class RepositoryPermissionFilter(
        private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
        private val securityService: SecurityService,
        @param:Qualifier("handlerExceptionResolver")
        private val resolver: HandlerExceptionResolver,
        private val repositoryHolder: RepositoryHolder,
        private val repositoryService: RepositoryService

) : OncePerRequestFilter() {

    companion object{
        const val REGEX = "/(?:v2|api)/repositor(?:y|ies)/([0-9]+)/?.*"
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val matchRegex = REGEX.toRegex()
        if (request.requestURI.matches(matchRegex)) {
            val specificPermissionAnnotation = getSpecificPermissionAnnotation(request)
            val anyPermissionAnnotation = getAnyPermissionAnnotation(request)

            if (specificPermissionAnnotation != null && anyPermissionAnnotation != null) {
                throw Exception("Cannot use both AccessWithRepositoryPermission" +
                        " and AccessWithAnyRepositoryPermission annotations.")
            }

            try {
                val repositoryId = request.repositoryId
                repositoryHolder.repository = repositoryService.get(repositoryId).orElseThrow { NotFoundException() }!!

                if (specificPermissionAnnotation != null) {
                    securityService.checkRepositoryPermission(repositoryId, specificPermissionAnnotation.permission)
                }

                if (anyPermissionAnnotation != null) {
                    securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.VIEW)
                }
            } catch (e: Exception) {
                resolver.resolveException(request, response, null, e)
                return
            }
        }
        filterChain.doFilter(request, response)
    }

    private val HttpServletRequest.repositoryId
        get() = this.requestURI
                .replace(REGEX.toRegex(), "$1").toLong()

    private fun getSpecificPermissionAnnotation(request: HttpServletRequest): AccessWithRepositoryPermission? {
        val handlerMethod = (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
        return handlerMethod?.getMethodAnnotation(AccessWithRepositoryPermission::class.java)
    }

    private fun getAnyPermissionAnnotation(request: HttpServletRequest): AccessWithAnyRepositoryPermission? {
        val handlerMethod = (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
        return handlerMethod?.getMethodAnnotation(AccessWithAnyRepositoryPermission::class.java)
    }
}
