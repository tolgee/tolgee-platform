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
import io.tolgee.util.Logging
import io.tolgee.util.SsrfSafeRequestFactoryProvider
import io.tolgee.util.logger
import jakarta.annotation.PreDestroy
import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.http.ClassicHttpResponse
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Fetches a Client ID Metadata Document (a client presenting an HTTPS URL as its `client_id`) from the addresses
 * [CimdHostResolver] pinned it to, returning the raw body. Nothing the document or its host can do produces an
 * exception — hostile input cannot 500 `/oauth2/authorize`. The one exception that does come out is the resolver's
 * [CimdNoCapacityException], which every caller must catch. Validating the body is the caller's job, and so is
 * admission: a request-lane fetch assumes [CimdFetchBudget] already granted a slot.
 */
@Component
class CimdDocumentFetcher(
  private val hostResolver: CimdHostResolver,
  private val internalProperties: InternalProperties,
) : Logging {
  // A drip-feeding host resets the per-read socket timeout on every byte, so only a total deadline bounds a fetch.
  private val watchdog =
    Executors.newSingleThreadScheduledExecutor { runnable ->
      Thread(runnable, "cimd-fetch-watchdog").apply { isDaemon = true }
    }

  @PreDestroy
  fun shutdown() {
    watchdog.shutdownNow()
  }

  fun fetch(
    clientIdUrl: String,
    lane: CimdFetchLane = CimdFetchLane.REQUEST,
  ): CimdDocument {
    if (!CimdUrls.isAcceptableClientId(clientIdUrl, allowHttp = internalProperties.disableUrlSsrfProtection)) {
      logger.debug("CIMD fetch refused for {}: not an acceptable client_id URL", clientIdUrl)
      return CimdDocument.Rejected
    }

    val addresses = hostResolver.resolve(clientIdUrl, lane) ?: return CimdDocument.Unavailable
    return fetchPinned(clientIdUrl, addresses)
  }

  internal fun fetchPinned(
    url: String,
    pinnedAddresses: List<InetAddress>,
    deadline: Duration = DEFAULT_DEADLINE,
    maxBytes: Int = MAX_DOCUMENT_BYTES,
  ): CimdDocument {
    pinnedHttpClient(pinnedAddresses).use { client ->
      val get = HttpGet(url)
      val deadlineNanos = System.nanoTime() + deadline.toNanos()
      // cancel() aborts immediately (CloseMode.IMMEDIATE); an ordinary close() would try to drain the unconsumed
      // body first, which on a drip-feeding host is exactly as unbounded as the read we are cutting off.
      val timeout = watchdog.schedule({ get.cancel() }, deadline.toMillis(), TimeUnit.MILLISECONDS)
      return try {
        client.execute(get) { response -> readDocument(url, response, maxBytes, deadlineNanos) }
      } catch (e: Exception) {
        logger.info("CIMD fetch failed for {}: {}", url, e.message)
        CimdDocument.Unavailable
      } finally {
        timeout.cancel(false)
      }
    }
  }

  private fun pinnedHttpClient(pinnedAddresses: List<InetAddress>): CloseableHttpClient {
    val pinningResolver =
      object : DnsResolver {
        override fun resolve(host: String): Array<InetAddress> = pinnedAddresses.toTypedArray()

        override fun resolveCanonicalHostname(host: String): String = host
      }
    return SsrfSafeRequestFactoryProvider
      .ssrfSafeClientBuilder(
        pinningResolver,
        CONNECT_TIMEOUT,
        RESPONSE_TIMEOUT,
        // An egress proxy would resolve the host itself and the DNS pin would never run.
        useSystemProperties = false,
      ).build()
  }

  private fun readDocument(
    url: String,
    response: ClassicHttpResponse,
    maxBytes: Int,
    deadlineNanos: Long,
  ): CimdDocument {
    if (response.code == 404 || response.code == 410) {
      logger.info("CIMD document gone for {}: responded {}", url, response.code)
      return CimdDocument.Gone
    }
    if (response.code != 200) {
      logger.info("CIMD document could not be read for {}: responded {}", url, response.code)
      return CimdDocument.Unavailable
    }
    val entity = response.entity ?: return CimdDocument.Unavailable
    // Any shared origin that serves user-supplied bytes at a stable GET URL - a raw file host, an object-storage
    // bucket, an upload path - could otherwise lend its origin to the consent screen.
    if (!isJson(entity.contentType)) {
      logger.info("CIMD document refused for {}: content type {}", url, entity.contentType)
      return CimdDocument.Rejected
    }
    val body = readCapped(entity.content, maxBytes, deadlineNanos)
    if (body == null) {
      logger.info("CIMD document could not be read for {}: body over {} bytes or past the deadline", url, maxBytes)
      return CimdDocument.Unavailable
    }
    return CimdDocument.Body(body)
  }

  /** `application/json`, or any `+json` structured suffix; parameters such as `; charset=utf-8` are ignored. */
  private fun isJson(contentType: String?): Boolean {
    val type = contentType?.substringBefore(';')?.trim()?.lowercase() ?: return false
    return type == "application/json" || type.endsWith("+json")
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
    const val MAX_DOCUMENT_BYTES = 256 * 1024
    private val CONNECT_TIMEOUT = Duration.ofSeconds(2)
    private val RESPONSE_TIMEOUT = Duration.ofSeconds(3)
    private val DEFAULT_DEADLINE = Duration.ofSeconds(4)
  }
}
