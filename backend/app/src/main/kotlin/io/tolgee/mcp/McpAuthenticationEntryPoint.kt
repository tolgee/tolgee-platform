package io.tolgee.mcp

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

// Spring Security's default Http403ForbiddenEntryPoint returns 403 for unauthenticated
// requests. RFC 7235 (and the MCP authorization spec) requires 401 + a WWW-Authenticate
// header when credentials are missing or invalid. This entry point provides that for the
// MCP path. We do not include an OAuth resource_metadata hint because Tolgee does not
// host an OAuth authorization server — the only auth schemes are PAT, PAK, and JWT.
@Component
class McpAuthenticationEntryPoint : AuthenticationEntryPoint {
  override fun commence(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authException: AuthenticationException,
  ) {
    response.status = HttpServletResponse.SC_UNAUTHORIZED
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, WWW_AUTHENTICATE_VALUE)
    response.contentType = MediaType.APPLICATION_JSON_VALUE
    response.writer.write("""{"error":"invalid_token","error_description":"Authentication required"}""")
  }

  companion object {
    const val WWW_AUTHENTICATE_VALUE = """Bearer realm="Tolgee MCP""""
  }
}
