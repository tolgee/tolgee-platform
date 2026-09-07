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
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.SequenceInputStream

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
    // A real tools/call envelope's `method` field sits in the first bytes, so anything past the cap is not worth
    // buffering to classify — buffering an unbounded, unauthenticated body here would double the memory the
    // transport already spends parsing it downstream.
    val peeked = request.inputStream.readNBytes(BODY_PEEK_CAP_BYTES + 1)
    if (peeked.size > BODY_PEEK_CAP_BYTES) {
      chain.doFilter(ReplayedPrefixRequest(request, peeked), response)
      return
    }
    if (!isToolsCall(peeked)) {
      chain.doFilter(CachedBodyRequest(request, peeked), response)
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
    val body: ByteArray,
  ) : HttpServletRequestWrapper(request) {
    override fun getInputStream(): ServletInputStream = wrap(ByteArrayInputStream(body))

    override fun getReader(): BufferedReader =
      BufferedReader(
        InputStreamReader(
          getInputStream(),
          characterEncoding ?: "UTF-8",
        ),
      )
  }

  // The body already exceeded the peek cap, so it is never parsed here — this only re-serves the bytes already
  // drained from the original stream (to classify them) ahead of whatever is still unread on it, so downstream
  // sees the exact same body it would have without this filter.
  private class ReplayedPrefixRequest(
    request: HttpServletRequest,
    prefix: ByteArray,
  ) : HttpServletRequestWrapper(request) {
    private val replayed = SequenceInputStream(ByteArrayInputStream(prefix), request.inputStream)

    override fun getInputStream(): ServletInputStream = wrap(replayed)

    override fun getReader(): BufferedReader =
      BufferedReader(
        InputStreamReader(
          getInputStream(),
          characterEncoding ?: "UTF-8",
        ),
      )
  }

  companion object {
    private const val BODY_PEEK_CAP_BYTES = 64 * 1024

    private fun wrap(source: InputStream): ServletInputStream =
      object : ServletInputStream() {
        override fun read(): Int = source.read()

        override fun isFinished(): Boolean = source.available() == 0

        override fun isReady(): Boolean = true

        override fun setReadListener(listener: ReadListener) = Unit
      }
  }
}
