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

package io.tolgee.security.oauth2.cimd

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.util.UrlSecurity
import jakarta.annotation.PreDestroy
import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.Timeout
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.URI
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Fetches a Client ID Metadata Document (a client presenting an HTTPS URL as its `client_id`). The fetch reaches an
 * attacker-named host, so it is SSRF-hardened, DNS-pinned and deadline-bounded, and it returns the raw body — never
 * throwing, so hostile input can never 500 `/oauth2/authorize`. Validating that body is the caller's job.
 *
 * The pin is the load-bearing part: [UrlSecurity.validateUrlAndResolve] resolves and validates the host once, and the
 * connection is pinned to exactly those addresses, so a zero-TTL rebinding host that answered a public IP to the
 * validator cannot answer an internal one at connect time.
 */
@Component
class CimdDocumentFetcher(
  private val urlSecurity: UrlSecurity,
  private val internalProperties: InternalProperties,
) {
  // Cancels a fetch that outran its wall-clock budget. A drip-feeding host resets per-read socket timeouts on every
  // byte, so only a total deadline bounds it; a daemon so it never holds shutdown.
  private val watchdog =
    Executors.newSingleThreadScheduledExecutor { runnable ->
      Thread(runnable, "cimd-fetch-watchdog").apply { isDaemon = true }
    }

  @PreDestroy
  fun shutdown() {
    watchdog.shutdownNow()
  }

  fun fetch(clientIdUrl: String): String? {
    // The grant's client_id column is VARCHAR(255); a longer-but-valid URL would pass here only to 500 at insert.
    if (clientIdUrl.length > MAX_CLIENT_ID_LENGTH) return null

    if (internalProperties.disableUrlSsrfProtection) {
      // Dev/e2e only: a locally-hosted CIMD client on http://localhost. Never a production path.
      val addresses = resolvePinned(clientIdUrl, allowLocalAddresses = true) ?: return null
      return fetchPinned(clientIdUrl, addresses)
    }

    if (!isHttpsUrl(clientIdUrl)) return null
    val addresses = resolvePinned(clientIdUrl, allowLocalAddresses = false) ?: return null
    return fetchPinned(clientIdUrl, addresses)
  }

  private fun resolvePinned(
    url: String,
    allowLocalAddresses: Boolean,
  ): List<InetAddress>? = runCatching { urlSecurity.validateUrlAndResolve(url, allowLocalAddresses) }.getOrNull()

  private fun isHttpsUrl(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    return uri.scheme?.lowercase() == "https"
  }

  internal fun fetchPinned(
    url: String,
    pinnedAddresses: List<InetAddress>,
    deadline: Duration = DEFAULT_DEADLINE,
    maxBytes: Int = MAX_DOCUMENT_BYTES,
  ): String? {
    // DnsResolver is a two-method interface in 5.x, not a SAM, so a lambda will not compile.
    val pinningResolver =
      object : DnsResolver {
        override fun resolve(host: String): Array<InetAddress> = pinnedAddresses.toTypedArray()

        override fun resolveCanonicalHostname(host: String): String = host
      }
    val connectionManager = PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(pinningResolver).build()
    val requestConfig =
      RequestConfig
        .custom()
        .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT_MS))
        .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT_MS))
        .setResponseTimeout(Timeout.ofMilliseconds(RESPONSE_TIMEOUT_MS))
        .setRedirectsEnabled(false)
        .build()

    HttpClients
      .custom()
      .setConnectionManager(connectionManager)
      .disableRedirectHandling()
      .disableAutomaticRetries()
      .disableCookieManagement()
      .setDefaultRequestConfig(requestConfig)
      .build()
      .use { client ->
        val get = HttpGet(url)
        val deadlineNanos = System.nanoTime() + deadline.toNanos()
        // cancel() aborts immediately (CloseMode.IMMEDIATE); an ordinary close() would try to drain the unconsumed
        // body first, which on a drip-feeding host is exactly as unbounded as the read we are cutting off.
        val timeout = watchdog.schedule({ get.cancel() }, deadline.toMillis(), TimeUnit.MILLISECONDS)
        return try {
          client.execute(get) { response ->
            if (response.code != 200) return@execute null
            val entity = response.entity ?: return@execute null
            readCapped(entity.content, maxBytes, deadlineNanos)
          }
        } catch (_: Exception) {
          null
        } finally {
          timeout.cancel(false)
        }
      }
  }

  private fun readCapped(
    input: InputStream,
    maxBytes: Int,
    deadlineNanos: Long,
  ): String? {
    val buffer = ByteArray(8192)
    val out = ByteArrayOutputStream()
    while (true) {
      if (System.nanoTime() > deadlineNanos) return null
      val read = input.read(buffer)
      if (read == -1) break
      if (out.size() + read > maxBytes) return null
      out.write(buffer, 0, read)
    }
    return out.toString(Charsets.UTF_8)
  }

  companion object {
    const val MAX_CLIENT_ID_LENGTH = 255
    const val MAX_DOCUMENT_BYTES = 256 * 1024
    private const val CONNECT_TIMEOUT_MS = 3_000L
    private const val RESPONSE_TIMEOUT_MS = 5_000L
    private val DEFAULT_DEADLINE = Duration.ofSeconds(8)
  }
}
