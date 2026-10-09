package io.tolgee.mcp

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.security.authentication.CredentialPresence
import io.tolgee.security.oauth2.OAuth2BearerChallengeProvider
import io.tolgee.util.textOrNull
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

/**
 * Answers a credential-less MCP call with an HTTP 401 and a `WWW-Authenticate` challenge, unless the call is one
 * the client may make before it logs in. `docs/oauth/README.md` says which calls those are and why. With
 * authentication turned off every call runs as the initial user, so nothing is challenged.
 */
class McpAuthChallengeFilter(
  private val challengeProvider: OAuth2BearerChallengeProvider,
  private val objectMapper: ObjectMapper,
  private val authenticationProperties: AuthenticationProperties,
) : OncePerRequestFilter() {
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    if (!authenticationProperties.enabled || request.method != "POST" || CredentialPresence.hasAny(request)) {
      filterChain.doFilter(request, response)
      return
    }

    val body = PeekedRequestBody.peek(request, PEEK_CAP)
    if (body.overCap || !isOpenToAnonymous(body.bytes)) {
      challenge(request, response)
      return
    }

    filterChain.doFilter(body.replay(), response)
  }

  private fun isOpenToAnonymous(body: ByteArray): Boolean {
    val root = runCatching { objectMapper.readTree(body) }.getOrNull() ?: return false
    val method = root.textOrNull("method") ?: return false
    return method in ANONYMOUS_METHODS || method.startsWith(NOTIFICATION_PREFIX)
  }

  private fun challenge(
    request: HttpServletRequest,
    response: HttpServletResponse,
  ) {
    response.status = HttpStatus.UNAUTHORIZED.value()
    val header = challengeProvider.challengeFor(request, HttpStatus.UNAUTHORIZED) ?: "Bearer"
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, header)
  }

  companion object {
    const val PEEK_CAP = 64 * 1024

    /** Anything not named here is challenged, so a new MCP method stays closed until it is added on purpose. */
    private val ANONYMOUS_METHODS = setOf("initialize", "tools/list", "ping")

    private const val NOTIFICATION_PREFIX = "notifications/"
  }
}
