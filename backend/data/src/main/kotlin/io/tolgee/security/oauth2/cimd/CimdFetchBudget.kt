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
import com.github.benmanes.caffeine.cache.Ticker
import io.tolgee.security.oauth2.UrlOrigins
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Limits how many requests may be waiting on a third-party server at once.
 *
 * Resolving an unknown `client_id` calls out to a server we do not control. That call can hang for seconds, and
 * while it hangs it holds a servlet thread. Anyone can start one without logging in, just by sending a `client_id`
 * nobody has seen before — so with no limit, enough of them hold every thread and Tolgee stops answering at all.
 * Callers past the limit are turned away immediately rather than queued, since queueing is the thing being avoided.
 *
 * A slot must be taken *before* [CimdClientCache] looks the key up, not around the fetch. Caffeine runs only one
 * fetch per key and parks every other caller for that key inside the map, so counting the fetch counts a single
 * thread while any number of others pile up behind it — and piling them up is exactly what repeating one
 * `client_id` does. The slot must also cover the DNS lookup, not just the HTTP request.
 */
@Component
class CimdFetchBudget(
  maxConcurrent: Int = MAX_CONCURRENT_FETCHES,
  maxConcurrentPerOrigin: Int = MAX_CONCURRENT_PER_ORIGIN,
  private val maxFetchesPerOriginPerMinute: Int = MAX_FETCHES_PER_ORIGIN_PER_MINUTE,
  ticker: Ticker = Ticker.systemTicker(),
) : Logging {
  private val inFlight = Semaphore(maxConcurrent)
  private val inFlightPerOrigin = CountingSlots(maxConcurrentPerOrigin)

  private val fetchesPerOrigin: Cache<String, AtomicInteger> =
    Caffeine
      .newBuilder()
      .maximumSize(RATE_WINDOW_ORIGINS)
      .expireAfterWrite(1, TimeUnit.MINUTES)
      .ticker(ticker)
      .build()

  /**
   * Runs [resolve] when there is room for the outbound call, and answers with whatever it returned. A `null` means
   * no document was read, whether this refused the call or [resolve] itself returned `null`.
   */
  fun <T : Any> withBudget(
    clientIdUrl: String,
    resolve: () -> T?,
  ): T? {
    val origin = UrlOrigins.originOf(clientIdUrl) ?: return null
    if (!admit(origin)) {
      logger.warn("CIMD resolution refused for {}: too many resolutions already in flight", clientIdUrl)
      return null
    }
    // Charged only after admission: a request the concurrency cap turned away sent the origin nothing, so charging
    // it would be a free way to spend a third party's quota.
    if (!withinRate(origin)) {
      finish(origin)
      logger.warn("CIMD resolution refused for {}: this origin is over its fetch rate for the minute", clientIdUrl)
      return null
    }
    return try {
      resolve()
    } finally {
      finish(origin)
    }
  }

  private fun withinRate(origin: String): Boolean =
    fetchesPerOrigin.get(origin) { AtomicInteger() }.incrementAndGet() <= maxFetchesPerOriginPerMinute

  private fun admit(origin: String): Boolean {
    if (!inFlightPerOrigin.take(origin)) return false
    if (inFlight.tryAcquire()) return true
    inFlightPerOrigin.release(origin)
    return false
  }

  private fun finish(origin: String) {
    inFlight.release()
    inFlightPerOrigin.release(origin)
  }

  companion object {
    const val MAX_CONCURRENT_FETCHES = 32
    const val MAX_CONCURRENT_PER_ORIGIN = 3
    const val MAX_FETCHES_PER_ORIGIN_PER_MINUTE = 120
    const val RATE_WINDOW_ORIGINS = 4_096L
  }
}
