package io.tolgee.security.api_key_auth

import io.tolgee.ExceptionHandlers
import io.tolgee.exceptions.PermissionException
import io.tolgee.service.ApiKeyService
import io.tolgee.service.SecurityService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class ApiAuthFilter(
        private val apiKeyService: ApiKeyService,
        private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
        private val securityService: SecurityService,
        private val exceptionHandlers: ExceptionHandlers
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        if (this.isApiAccessAllowed(request)) {
            val keyParameter = request.getParameter("ak")
            if (keyParameter != null && !keyParameter.isEmpty()) {
                val ak = apiKeyService.getApiKey(keyParameter)
                if (ak.isPresent) {
                    val apiKeyAuthenticationToken = ApiKeyAuthenticationToken(ak.get())
                    SecurityContextHolder.getContext().authentication = apiKeyAuthenticationToken

                    val apiScopes = this.getAccessAllowedAnnotation(request)!!.scopes
                    try {
                        securityService.checkApiKeyScopes(setOf(*apiScopes), ak.get())
                    } catch (e: PermissionException) {
                        val errorResponseEntity = exceptionHandlers.handleServerError(e);
                        response.status = errorResponseEntity.statusCodeValue;
                        response.outputStream.print(errorResponseEntity?.body?.toString());
                        return;
                    }
                }
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun isApiAccessAllowed(request: HttpServletRequest): Boolean {
        return this.getAccessAllowedAnnotation(request) != null
    }

    private fun getAccessAllowedAnnotation(request: HttpServletRequest): AllowAccessWithApiKey? {
        return (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
                ?.getMethodAnnotation(AllowAccessWithApiKey::class.java) ?: return null
    }
}