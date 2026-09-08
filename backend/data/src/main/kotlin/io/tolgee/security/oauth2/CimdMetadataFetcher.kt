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

package io.tolgee.security.oauth2

import io.tolgee.configuration.tolgee.OAuth2CimdProperties
import io.tolgee.util.Logging
import io.tolgee.util.UrlSecurity
import io.tolgee.util.logger
import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.Timeout
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Component
class CimdMetadataFetcher(
  private val properties: OAuth2CimdProperties,
  private val urlSecurity: UrlSecurity,
  private val objectMapper: ObjectMapper,
) : Logging {
  // A per-read socket timeout resets on every successful partial read, so it never bounds the total time spent
  // reading a body that trickles in slower than the cap — and closing an entity that wasn't fully read (readCapped
  // aborting early) makes HttpClient5 try to drain the rest of it to keep the connection reusable, which is just
  // as unbounded. This watchdog is what actually enforces fetchTimeoutMs as a wall-clock deadline: cancelling the
  // request forces the connection closed immediately (CloseMode.IMMEDIATE) instead of gracefully drained, which
  // aborts whichever blocking call — read or drain — is in flight at the time.
  private val fetchWatchdog: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor { runnable ->
      Thread(runnable, "cimd-fetch-watchdog").apply { isDaemon = true }
    }

  fun fetchAndValidate(clientIdUrl: String): CimdClient? {
    if (!isSafeUrl(clientIdUrl)) return null
    val addresses =
      try {
        urlSecurity.validateUrlAndResolve(clientIdUrl)
      } catch (_: Exception) {
        return null
      }

    val document = fetch(clientIdUrl, addresses) ?: return null
    // Defense in depth: buildClient already reads every field fail-closed, but it builds a client out of untrusted
    // metadata, so any residual throw must still resolve to null rather than surface as a 500 on /oauth2/authorize.
    return try {
      buildClient(clientIdUrl, document)
    } catch (e: Exception) {
      logger.debug("CIMD document rejected for {}: {}", clientIdUrl, e.message)
      null
    }
  }

  internal fun isSafeUrl(clientIdUrl: String): Boolean {
    val uri =
      try {
        URI(clientIdUrl)
      } catch (_: Exception) {
        return false
      }
    if (uri.scheme?.lowercase() != "https") return false
    // oauth2_grant.client_id is VARCHAR(255); a longer client_id url would 500 at grant-insert time instead of
    // being refused up front here.
    if (clientIdUrl.length > 255) return false
    val host = uri.host ?: return false
    if (properties.allowedHosts.isNotEmpty() &&
      properties.allowedHosts.none { it.equals(host, ignoreCase = true) }
    ) {
      return false
    }
    return true
  }

  // pinnedAddresses is null exactly when UrlSecurity resolved nothing to pin to (SSRF protection disabled for
  // local dev/E2E) — otherwise it is the very array UrlSecurity just validated, and pinning the connection to it
  // is what closes the window between that validation and this connect: a second, independent DNS lookup here
  // could return a different (attacker-controlled) address than the one already checked.
  internal fun fetch(
    clientIdUrl: String,
    pinnedAddresses: Array<InetAddress>?,
  ): JsonNode? {
    val host = URI(clientIdUrl).host ?: return null
    // System.nanoTime, not CurrentDateProvider: this is a wall-clock budget for an outbound socket read loop,
    // not domain time that tests need to freeze/travel.
    val deadlineNanos = System.nanoTime() + properties.fetchTimeoutMs * NANOS_PER_MILLI
    val request = HttpGet(clientIdUrl).apply { addHeader("Accept", "application/json") }
    val watchdogTask = fetchWatchdog.schedule({ request.cancel() }, properties.fetchTimeoutMs, TimeUnit.MILLISECONDS)
    return try {
      pinnedClient(host, pinnedAddresses).use { client ->
        client.execute(request) { response ->
          if (response.code != 200) return@execute null
          val bytes = response.entity?.content?.use { readCapped(it, deadlineNanos) } ?: return@execute null
          objectMapper.readTree(bytes)
        }
      }
    } catch (e: Exception) {
      logger.debug("CIMD fetch failed for {}: {}", clientIdUrl, e.message)
      null
    } finally {
      watchdogTask.cancel(false)
    }
  }

  private fun pinnedClient(
    host: String,
    addresses: Array<InetAddress>?,
  ): CloseableHttpClient {
    val timeout = Timeout.ofMilliseconds(properties.fetchTimeoutMs)
    val connectionManagerBuilder =
      PoolingHttpClientConnectionManagerBuilder
        .create()
        .setDefaultConnectionConfig(
          ConnectionConfig
            .custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .build(),
        )
    if (addresses != null) {
      connectionManagerBuilder.setDnsResolver(pinnedResolver(host, addresses))
    }
    return HttpClients
      .custom()
      .setConnectionManager(connectionManagerBuilder.build())
      // Bounds the wait for the response to start arriving; readCapped's own deadline bounds a slow body once it
      // has, since a per-read socket timeout resets on every trickled byte and never fires on its own.
      .setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(timeout).build())
      .disableRedirectHandling()
      .disableAutomaticRetries()
      .build()
  }

  private fun pinnedResolver(
    host: String,
    addresses: Array<InetAddress>,
  ): DnsResolver =
    object : DnsResolver {
      override fun resolve(resolvedHost: String): Array<InetAddress> {
        if (!resolvedHost.equals(host, ignoreCase = true)) throw UnknownHostException(resolvedHost)
        return addresses
      }

      override fun resolveCanonicalHostname(resolvedHost: String): String = resolvedHost
    }

  // A single readNBytes(max+1) call blocks on the underlying socket read, whose timeout resets on every
  // successful partial read — a host dripping one byte per read call never trips it and can hold the calling
  // thread indefinitely. Reading in bounded chunks and checking deadlineNanos between them makes fetchTimeoutMs
  // the total wall-clock budget for the whole body, not just the gap between two bytes.
  internal fun readCapped(
    stream: InputStream,
    deadlineNanos: Long,
  ): ByteArray? {
    val max = properties.maxDocumentBytes.toInt()
    val buffer = ByteArrayOutputStream()
    val chunk = ByteArray(READ_CHUNK_BYTES)
    while (true) {
      if (System.nanoTime() >= deadlineNanos) return null
      val read = stream.read(chunk)
      if (read < 0) break
      buffer.write(chunk, 0, read)
      if (buffer.size() > max) return null
    }
    return buffer.toByteArray()
  }

  internal fun buildClient(
    clientIdUrl: String,
    document: JsonNode,
  ): CimdClient? {
    if (document.get("client_id").textOrNull() != clientIdUrl) return null
    if (document.get("token_endpoint_auth_method").textOrNull() != "none") return null

    val grantTypes = stringList(document.get("grant_types")) ?: return null
    if (grantTypes.isNotEmpty() && !grantTypes.contains("authorization_code")) return null

    val redirectUris = stringList(document.get("redirect_uris")) ?: return null
    if (redirectUris.isEmpty()) return null
    if (!redirectUris.all { isAcceptableRedirect(it, clientIdUrl) }) return null

    val client =
      OAuth2Client(
        clientId = clientIdUrl,
        name = document.get("client_name").textOrNull() ?: clientIdUrl,
        redirectUris = redirectUris,
        verified = false,
        metadataHash = metadataHash(clientIdUrl, redirectUris),
      )
    return CimdClient(client, sameOriginLogo(document, clientIdUrl), client.metadataHash!!)
  }

  private fun sameOriginLogo(
    document: JsonNode,
    clientIdUrl: String,
  ): String? {
    val logo = document.get("logo_uri").textOrNull() ?: return null
    if (!isSameOrigin(logo, clientIdUrl)) return null
    return logo
  }

  // Same-origin https, or a loopback (native MCP clients like Claude Code listen on localhost). Anything else —
  // a third https origin, a custom scheme — is where a hostile document would point a code, so it is refused.
  private fun isAcceptableRedirect(
    redirectUri: String,
    clientIdUrl: String,
  ): Boolean {
    // RFC 6749 §3.1.2: a registered redirect_uri must not carry a fragment component.
    val uri = runCatching { URI(redirectUri) }.getOrNull() ?: return false
    if (uri.rawFragment != null) return false
    if (isSameOrigin(redirectUri, clientIdUrl)) return true
    if (uri.scheme != "http" && uri.scheme != "https") return false
    return OAuth2Client.isLoopbackHost(uri.host)
  }

  private fun isSameOrigin(
    redirectUri: String,
    clientIdUrl: String,
  ): Boolean {
    return try {
      val a = URI(redirectUri)
      val b = URI(clientIdUrl)
      a.scheme == b.scheme && a.host == b.host && effectivePort(a) == effectivePort(b)
    } catch (_: Exception) {
      false
    }
  }

  // https-only, so an omitted port (-1) is the same origin as an explicit :443.
  private fun effectivePort(uri: URI): Int {
    if (uri.port == -1) return 443
    return uri.port
  }

  private fun metadataHash(
    clientIdUrl: String,
    redirectUris: List<String>,
  ): String {
    val material = clientIdUrl + "\n" + redirectUris.sorted().joinToString("\n")
    val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  // A field that is present but not a well-formed string array (wrong shape, or any element that isn't a plain
  // string) must reject the whole document rather than silently drop the bad entries — a mix of one valid and one
  // container-valued redirect_uri would otherwise register the valid one while hiding that the document is bogus.
  private fun stringList(node: JsonNode?): List<String>? {
    if (node == null) return emptyList()
    if (!node.isArray) return null
    val values = mutableListOf<String>()
    for (i in 0 until node.size()) {
      values += node.get(i).textOrNull() ?: return null
    }
    return values
  }

  // Jackson's asString() coerces scalars but throws on a container node (object/array), so an attacker-supplied field
  // given as `{...}`/`[...]` would otherwise escape buildClient. Read every string field through this instead.
  private fun JsonNode?.textOrNull(): String? {
    val node = this ?: return null
    if (!node.isValueNode) return null
    return try {
      node.asString()
    } catch (_: Exception) {
      null
    }
  }

  companion object {
    private const val READ_CHUNK_BYTES = 8192
    private const val NANOS_PER_MILLI = 1_000_000L
  }
}
