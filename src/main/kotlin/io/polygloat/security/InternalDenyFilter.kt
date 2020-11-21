package io.polygloat.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class InternalDenyFilter(
        @Value("\${app.allowInternal:false}")
        val allowInternal: String,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        if (isInternal(request) && !isInternalAllowed()) {
            response.status = 403
            response.outputStream.print("Internal access is not allowed")
            return;
        }
        filterChain.doFilter(request, response);
    }

    private fun isInternal(request: HttpServletRequest): Boolean {
        return request.requestURI.startsWith("/internal");
    }

    private fun isInternalAllowed() = allowInternal == "true"
}