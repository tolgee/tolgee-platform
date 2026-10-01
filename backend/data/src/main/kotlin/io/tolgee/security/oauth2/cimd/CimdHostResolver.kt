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
import io.tolgee.util.UrlSecurity
import io.tolgee.util.logger
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Resolves the host of a `client_id` URL to the addresses a fetch is pinned to, through
 * [UrlSecurity.validateUrlAndResolve], and decides who may spend a resolver thread on it.
 *
 * `InetAddress.getAllByName` has no timeout and cannot be interrupted: a nameserver that drops queries holds the
 * thread for tens of seconds. That is why the lookup runs on its own thread with a deadline — the resolver thread
 * stays stuck until the OS gives up, but the caller does not wait with it. The one exception raised is about us, not
 * the host: [CimdNoCapacityException] when no thread may be spent, which every caller must catch.
 */
@Component
class CimdHostResolver(
  private val urlSecurity: UrlSecurity,
  private val internalProperties: InternalProperties,
  private val metrics: Metrics,
) : Logging {
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
    resolvers.shutdownNow()
    grantResolvers.shutdownNow()
  }

  /** The pinned addresses, or null when the host is unresolvable, blocked, or did not answer within the deadline. */
  fun resolve(
    url: String,
    lane: CimdFetchLane,
  ): List<InetAddress>? {
    val host = UrlOrigins.parse(url)?.host?.lowercase() ?: return null
    val stuckHosts =
      when (lane) {
        CimdFetchLane.REQUEST -> recentlyStuckHosts
        CimdFetchLane.GRANT_CHECK -> recentlyStuckHostsForCheck
      }
    if (stuckHosts.getIfPresent(host) != null) {
      logger.info("CIMD fetch refused for {}: this host's last lookup ran past the deadline", url)
      metrics.oauth2CimdCapacityRefusalsCounter.increment()
      return null
    }
    val pool =
      when (lane) {
        CimdFetchLane.REQUEST -> resolvers
        CimdFetchLane.GRANT_CHECK -> grantResolvers
      }
    // The check reads one document at a time, so its lane needs no per-host cap.
    val live =
      when (lane) {
        CimdFetchLane.REQUEST -> liveResolutions
        CimdFetchLane.GRANT_CHECK -> null
      }
    if (live != null && !live.take(host)) {
      logger.info("CIMD fetch refused for {}: this host already holds its share of the resolvers", url)
      throw CimdNoCapacityException(url)
    }
    val allowLocalAddresses = internalProperties.disableUrlSsrfProtection
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

  companion object {
    private val RESOLVE_DEADLINE = Duration.ofSeconds(2)
    internal const val MAX_CONCURRENT_RESOLUTIONS = 16
    private const val MAX_GRANT_RESOLUTIONS = 8
    internal const val MAX_RESOLUTIONS_PER_HOST = 4
    private val STUCK_HOST_MEMO_TTL = Duration.ofSeconds(30)
    private const val STUCK_HOST_MEMO_ENTRIES = 4_096L
  }
}
