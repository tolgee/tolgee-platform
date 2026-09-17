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

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.tolgee.Metrics
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.security.oauth2.UrlOrigins
import io.tolgee.util.Logging
import io.tolgee.util.SsrfSafeRequestFactoryProvider
import io.tolgee.util.UrlSecurity
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Fetches a Client ID Metadata Document (a client presenting an HTTPS URL as its `client_id`) through
 * [UrlSecurity.validateUrlAndResolve], returning the raw body. Nothing the document or its host can do produces an
 * exception — hostile input cannot 500 `/oauth2/authorize`. The one exception it does raise is about us, not them:
 * [CimdNoCapacityException] when no resolver slot is free, which every caller must catch. Validating the body is
 * the caller's job, and so is admission: a request-lane fetch assumes [CimdFetchBudget] already granted a slot.
 */
@Component
class CimdDocumentFetcher(
  private val urlSecurity: UrlSecurity,
  private val internalProperties: InternalProperties,
  private val metrics: Metrics,
) : Logging {
  // A drip-feeding host resets the per-read socket timeout on every byte, so only a total deadline bounds a fetch.
  private val watchdog =
    Executors.newSingleThreadScheduledExecutor { runnable ->
      Thread(runnable, "cimd-fetch-watchdog").apply { isDaemon = true }
    }

  private val resolvers = resolverPool(MAX_CONCURRENT_RESOLUTIONS, "cimd-resolve")

  /** Threads only a lookup for an existing grant may use. */
  private val grantResolvers = resolverPool(MAX_GRANT_RESOLUTIONS, "cimd-resolve-grant")

  /**
   * Resolver threads alive per host, counted for the thread's real life and not for the caller's wait, which ends
   * much sooner. Keyed on the host and not the origin, because ports are free.
   */
  private val liveResolutions = CountingSlots(MAX_RESOLUTIONS_PER_HOST)

  /**
   * Hosts whose lookup just ran past the deadline, refused for a while without a thread. One memo per lane: a
   * shared one would let a request decide what the background check may read.
   */
  private val recentlyStuckHosts: Cache<String, Boolean> = stuckHostMemo()
  private val recentlyStuckHostsForCheck: Cache<String, Boolean> = stuckHostMemo()

  private fun stuckHostMemo(): Cache<String, Boolean> =
    Caffeine
      .newBuilder()
      .maximumSize(STUCK_HOST_MEMO_ENTRIES)
      .expireAfterWrite(STUCK_HOST_MEMO_TTL)
      .build()

  private fun resolverPool(
    size: Int,
    name: String,
  ) = ThreadPoolExecutor(
    0,
    size,
    30L,
    TimeUnit.SECONDS,
    SynchronousQueue(),
  ) { runnable -> Thread(runnable, name).apply { isDaemon = true } }

  @PreDestroy
  fun shutdown() {
    watchdog.shutdownNow()
    resolvers.shutdownNow()
    grantResolvers.shutdownNow()
  }

  fun fetch(
    clientIdUrl: String,
    forExistingGrant: Boolean = false,
  ): CimdDocument {
    val allowLocalAddresses = internalProperties.disableUrlSsrfProtection
    if (!CimdUrls.isAcceptableClientId(clientIdUrl, allowHttp = allowLocalAddresses)) {
      logger.debug("CIMD fetch refused for {}: not an acceptable client_id URL", clientIdUrl)
      return CimdDocument.Rejected
    }

    val addresses = resolvePinned(clientIdUrl, allowLocalAddresses, forExistingGrant) ?: return CimdDocument.Unavailable
    return fetchPinned(clientIdUrl, addresses)
  }

  /**
   * `InetAddress.getAllByName` has no timeout and cannot be interrupted: a nameserver that drops queries holds the
   * thread for tens of seconds. That is why the lookup runs on its own thread with a deadline here — the resolver
   * thread stays stuck until the OS gives up, but the budget slot and the request thread do not wait with it.
   */
  private fun resolvePinned(
    url: String,
    allowLocalAddresses: Boolean,
    forExistingGrant: Boolean,
  ): List<InetAddress>? {
    val host = UrlOrigins.parse(url)?.host?.lowercase() ?: return null
    val stuckHosts = if (forExistingGrant) recentlyStuckHostsForCheck else recentlyStuckHosts
    if (stuckHosts.getIfPresent(host) != null) {
      logger.info("CIMD fetch refused for {}: this host's last lookup ran past the deadline", url)
      metrics.oauth2CimdCapacityRefusalsCounter.increment()
      return null
    }
    val pool = if (forExistingGrant) grantResolvers else resolvers
    // The check reads one document at a time, so its lane needs no per-host cap.
    val live = if (forExistingGrant) null else liveResolutions
    if (live != null && !live.take(host)) {
      logger.info("CIMD fetch refused for {}: this host already holds its share of the resolvers", url)
      throw CimdNoCapacityException(url)
    }
    val resolution = CompletableFuture<List<InetAddress>>()
    try {
      // execute() and not submit(): a submitted task cancelled before it starts never runs, so it would leak a slot.
      pool.execute {
        try {
          resolution.complete(urlSecurity.validateUrlAndResolve(url, allowLocalAddresses))
        } catch (e: Throwable) {
          resolution.completeExceptionally(e)
        } finally {
          live?.release(host)
        }
      }
    } catch (_: RejectedExecutionException) {
      live?.release(host)
      logger.info("CIMD fetch refused for {}: no resolver capacity", url)
      throw CimdNoCapacityException(url)
    }
    return try {
      resolution.get(RESOLVE_DEADLINE.toMillis(), TimeUnit.MILLISECONDS)
    } catch (_: TimeoutException) {
      stuckHosts.put(host, true)
      logger.info("CIMD fetch refused for {}: host did not resolve within {}", url, RESOLVE_DEADLINE)
      metrics.oauth2CimdCapacityRefusalsCounter.increment()
      null
    } catch (e: Exception) {
      logger.info("CIMD fetch refused for {}: host is unresolvable or blocked ({})", url, e.cause?.message ?: e.message)
      null
    }
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

    private val RESOLVE_DEADLINE = Duration.ofSeconds(2)
    internal const val MAX_CONCURRENT_RESOLUTIONS = 16
    private const val MAX_GRANT_RESOLUTIONS = 8
    internal const val MAX_RESOLUTIONS_PER_HOST = 4
    private val STUCK_HOST_MEMO_TTL = Duration.ofSeconds(30)
    private const val STUCK_HOST_MEMO_ENTRIES = 4_096L
  }
}
