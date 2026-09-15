/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.mcp

import io.tolgee.security.oauth2.OAuth2BearerChallengeProvider
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
import java.io.SequenceInputStream

/**
 * The MCP endpoint is a RouterFunction, so a `tools/call` that carries no credential fails deep inside the tool
 * handler as an in-band JSON-RPC error — which a client's OAuth machinery never reads. This filter turns that one
 * case into the HTTP 401 + `WWW-Authenticate` challenge a spec-following client acts on, so a fresh MCP client can
 * discover it must log in.
 *
 * `initialize`, `tools/list` and `ping` stay public deliberately (the public tools list is a feature); any request
 * already carrying a credential is left for [io.tolgee.security.authentication.AuthenticationFilter] to judge.
 */
class McpAuthChallengeFilter(
  private val challengeProvider: OAuth2BearerChallengeProvider,
  private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: jakarta.servlet.FilterChain,
  ) {
    // Only the JSON-RPC POST is a tools/call; the SSE GET stream and the session-teardown DELETE are not, and reading
    // their bodies would be wrong.
    if (request.method != "POST" || hasAnyCredential(request)) {
      filterChain.doFilter(request, response)
      return
    }

    // Read one byte past the cap so an exactly-cap body still parses while a larger one is detectably over.
    val prefix = ByteArray(PEEK_CAP + 1)
    val read = readFully(request.inputStream, prefix)
    val peeked = prefix.copyOf(read)

    // Over the cap we never parsed the body, so we do not know its method and deliberately synthesize no decision from
    // it: the request is handed to the transport unchanged, exactly as before this filter existed. The cap is generous
    // (every public method — initialize, tools/list, ping — is well under 1KB), so a body this large is not a normal
    // discovery request; whatever it is, the downstream authorizer still rejects an anonymous tools/call of any size,
    // so only the discovery-hint 401 is forgone, never the authorization. Replay the already-read prefix ahead of the
    // untouched remainder without buffering the whole body: the peek runs on an unauthenticated surface, so the read
    // this filter itself performs must stay bounded.
    if (read > PEEK_CAP) {
      val replay = SequenceInputStream(ByteArrayInputStream(peeked), request.inputStream)
      filterChain.doFilter(StreamingReplayRequest(request, replay), response)
      return
    }

    if (isCredentiallessToolsCall(peeked)) {
      challenge(request, response)
      return
    }

    filterChain.doFilter(BufferedReplayRequest(request, peeked), response)
  }

  private fun isCredentiallessToolsCall(body: ByteArray): Boolean {
    val root = runCatching { objectMapper.readTree(body) }.getOrNull() ?: return false
    return methodOf(root) == "tools/call"
  }

  // A batch (a JSON array) is not a single method call; the 2025-06-18 spec removed batching, so it degrades to the
  // transport's own in-band error rather than a challenge. asString throws on a container node, hence the value check.
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

  /**
   * The three forms [io.tolgee.security.authentication.AuthenticationFilter] accepts. The `?ak=` key is read out of
   * the raw query string by hand: `getParameter` would parse — and so consume — a form-encoded body before this
   * filter peeks it.
   */
  private fun hasAnyCredential(request: HttpServletRequest): Boolean {
    if (request.getHeader("Authorization") != null) return true
    if (request.getHeader("X-API-Key") != null) return true
    return hasAkQueryParameter(request.queryString)
  }

  private fun hasAkQueryParameter(queryString: String?): Boolean {
    val query = queryString ?: return false
    return query.split("&").any { it == "ak" || it.startsWith("ak=") }
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
    // 64KB: a tools/call envelope's method is near the front, and the surface is unauthenticated.
    const val PEEK_CAP = 64 * 1024
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

/**
 * Serves an over-cap body once, without buffering it: the already-read prefix concatenated with the untouched
 * remainder. Reads flow to whichever of [getInputStream]/[getReader] the transport reaches for first.
 */
private class StreamingReplayRequest(
  request: HttpServletRequest,
  private val body: InputStream,
) : HttpServletRequestWrapper(request) {
  private val stream by lazy { ReplayServletInputStream(body) }

  override fun getInputStream(): ServletInputStream = stream

  override fun getReader(): BufferedReader =
    BufferedReader(InputStreamReader(stream, characterEncoding ?: Charsets.UTF_8.name()))
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
