package io.tolgee.mcp

import io.tolgee.security.authentication.CredentialPresence
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
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Answers a credential-less MCP call with an HTTP 401 and a `WWW-Authenticate` challenge, unless the call is one
 * the client may make before it logs in. `docs/oauth/README.md` says which calls those are and why.
 */
class McpAuthChallengeFilter(
  private val challengeProvider: OAuth2BearerChallengeProvider,
  private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    if (request.method != "POST" || CredentialPresence.hasAny(request)) {
      filterChain.doFilter(request, response)
      return
    }

    // Read one byte past the cap so an exactly-cap body still parses while a larger one is detectably over.
    val prefix = ByteArray(PEEK_CAP + 1)
    val read = readFully(request.inputStream, prefix)
    val peeked = prefix.copyOf(read)

    if (read > PEEK_CAP || !isOpenToAnonymous(peeked)) {
      challenge(request, response)
      return
    }

    filterChain.doFilter(BufferedReplayRequest(request, peeked), response)
  }

  private fun isOpenToAnonymous(body: ByteArray): Boolean {
    val root = runCatching { objectMapper.readTree(body) }.getOrNull() ?: return false
    val method = methodOf(root) ?: return false
    return method in ANONYMOUS_METHODS || method.startsWith(NOTIFICATION_PREFIX)
  }

  // asString throws on a container node in Jackson 3, so a non-value (a batch array included) is treated as absent.
  private fun methodOf(root: JsonNode): String? {
    val method = root.get("method") ?: return null
    if (!method.isValueNode) return null
    return method.asString()
  }

  private fun challenge(
    request: HttpServletRequest,
    response: HttpServletResponse,
  ) {
    response.status = HttpStatus.UNAUTHORIZED.value()
    val header = challengeProvider.challengeFor(request, HttpStatus.UNAUTHORIZED) ?: "Bearer"
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, header)
  }

  private fun readFully(
    source: InputStream,
    into: ByteArray,
  ): Int {
    var total = 0
    while (total < into.size) {
      val n = source.read(into, total, into.size - total)
      if (n == -1) break
      total += n
    }
    return total
  }

  companion object {
    const val PEEK_CAP = 64 * 1024

    /** Anything not named here is challenged, so a new MCP method stays closed until it is added on purpose. */
    private val ANONYMOUS_METHODS = setOf("initialize", "tools/list", "ping")

    private const val NOTIFICATION_PREFIX = "notifications/"
  }
}

/**
 * Re-serves a fully-read body through both [getInputStream] and [getReader], afresh on every call — the transport
 * reads the stream, but an unoverridden [getReader] would hand it a drained one.
 */
private class BufferedReplayRequest(
  request: HttpServletRequest,
  private val buffered: ByteArray,
) : HttpServletRequestWrapper(request) {
  private val charset get() = characterEncoding ?: Charsets.UTF_8.name()

  override fun getInputStream(): ServletInputStream = ReplayServletInputStream(ByteArrayInputStream(buffered))

  override fun getReader(): BufferedReader = BufferedReader(InputStreamReader(ByteArrayInputStream(buffered), charset))
}

private class ReplayServletInputStream(
  private val delegate: InputStream,
) : ServletInputStream() {
  override fun read(): Int = delegate.read()

  override fun read(
    b: ByteArray,
    off: Int,
    len: Int,
  ): Int = delegate.read(b, off, len)

  override fun isFinished(): Boolean = delegate.available() == 0

  override fun isReady(): Boolean = true

  override fun setReadListener(listener: ReadListener?) {
    throw UnsupportedOperationException()
  }
}
