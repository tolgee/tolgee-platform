package io.tolgee.mcp

import io.tolgee.security.oauth2.OAuth2BearerChallengeProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream

/**
 * RFC 9728 §5.1 / MCP authorization: a client that has never authenticated only learns it must — and where the
 * flow starts — from an HTTP 401 carrying `WWW-Authenticate`. The MCP transport itself answers in-band JSON-RPC
 * errors, which no client's auth machinery reads, so the refusal has to happen before dispatch. Discovery methods
 * stay anonymous; only invoking a tool demands a credential.
 */
class McpAuthChallengeFilter(
  private val challengeProvider: OAuth2BearerChallengeProvider,
  private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    chain: FilterChain,
  ) {
    if (request.method != "POST" || hasCredentials(request)) {
      chain.doFilter(request, response)
      return
    }
    val wrapped = CachedBodyRequest(request)
    if (!isToolsCall(wrapped.body)) {
      chain.doFilter(wrapped, response)
      return
    }
    response.status = HttpStatus.UNAUTHORIZED.value()
    challengeProvider.challengeFor(request, HttpStatus.UNAUTHORIZED)?.let {
      response.setHeader(HttpHeaders.WWW_AUTHENTICATE, it)
    }
  }

  private fun hasCredentials(request: HttpServletRequest): Boolean =
    request.getHeader(HttpHeaders.AUTHORIZATION) != null || request.getHeader("X-API-Key") != null

  private fun isToolsCall(body: ByteArray): Boolean {
    val method =
      runCatching { objectMapper.readTree(body).get("method")?.asString() }.getOrNull()
        ?: return false
    return method == "tools/call"
  }

  private class CachedBodyRequest(
    request: HttpServletRequest,
  ) : HttpServletRequestWrapper(request) {
    val body: ByteArray = request.inputStream.readAllBytes()

    override fun getInputStream(): ServletInputStream {
      val source = ByteArrayInputStream(body)
      return object : ServletInputStream() {
        override fun read(): Int = source.read()

        override fun isFinished(): Boolean = source.available() == 0

        override fun isReady(): Boolean = true

        override fun setReadListener(listener: ReadListener) = Unit
      }
    }
  }
}
