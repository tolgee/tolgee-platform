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
import com.github.benmanes.caffeine.cache.Expiry
import com.github.benmanes.caffeine.cache.Ticker
import io.tolgee.Metrics
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class CimdClientCache(
  private val metadataFetcher: CimdMetadataFetcher,
  private val metrics: Metrics,
  private val budget: CimdFetchBudget,
  approximateMaxEntries: Long = MAX_ENTRIES,
  positiveTtlSeconds: Long = POSITIVE_TTL_SECONDS,
  rejectedTtlSeconds: Long = REJECTED_TTL_SECONDS,
  unavailableTtlSeconds: Long = UNAVAILABLE_TTL_SECONDS,
  ticker: Ticker = Ticker.systemTicker(),
) {
  private val cache: Cache<String, CimdResolution> =
    buildCache(approximateMaxEntries, positiveTtlSeconds, rejectedTtlSeconds, unavailableTtlSeconds, ticker)

  fun get(clientIdUrl: String): CimdClient? = resolveForRequest(clientIdUrl).clientOrNull()

  /**
   * Fetches the publisher's document now, on the lane kept for clients that already have a grant: no cache on
   * either side of the call, and nothing taken from [CimdFetchBudget], which only bounds request traffic. The
   * background check is its only caller: it has to see what the publisher serves at this moment, and what it reads
   * must not warm the lane `/oauth2/authorize` answers from — see `docs/oauth/README.md`.
   */
  fun fetchOnGrantLane(clientIdUrl: String): CimdResolution {
    val resolution =
      try {
        metadataFetcher.fetchAndValidate(clientIdUrl, forExistingGrant = true)
      } catch (_: CimdNoCapacityException) {
        null
      }
    return resolution ?: refuseForNoCapacity()
  }

  /** What is already known, with no outbound call: for callers that run on every request. */
  fun cachedResolution(clientIdUrl: String): CimdResolution? = cache.getIfPresent(clientIdUrl)

  fun invalidate(clientIdUrl: String) {
    cache.invalidate(clientIdUrl)
  }

  /**
   * A miss takes a budget slot before entering [Cache.get], never inside it — see [CimdFetchBudget] for why.
   */
  private fun resolveForRequest(clientIdUrl: String): CimdResolution {
    cache.getIfPresent(clientIdUrl)?.let { return it }
    val resolution =
      budget.withBudget(clientIdUrl) {
        try {
          cache.get(clientIdUrl) { metadataFetcher.fetchAndValidate(it) }
        } catch (_: CimdNoCapacityException) {
          // Nothing is stored: caching a capacity refusal would let whoever filled the pool decide what we believe
          // about this client for the whole negative TTL.
          null
        }
      }
    return resolution ?: refuseForNoCapacity()
  }

  private fun refuseForNoCapacity(): CimdResolution {
    metrics.oauth2CimdCapacityRefusalsCounter.increment()
    return CimdResolution.Unavailable
  }

  private fun CimdResolution.clientOrNull(): CimdClient? = (this as? CimdResolution.Resolved)?.client

  private fun buildCache(
    approximateMaxEntries: Long,
    positiveTtlSeconds: Long,
    rejectedTtlSeconds: Long,
    unavailableTtlSeconds: Long,
    ticker: Ticker,
  ): Cache<String, CimdResolution> =
    Caffeine
      .newBuilder()
      .maximumWeight(approximateMaxEntries * AVERAGE_ENTRY_WEIGHT)
      .weigher { key: String, value: CimdResolution -> key.length + weigh(value) }
      .ticker(ticker)
      .expireAfter(
        object : Expiry<String, CimdResolution> {
          override fun expireAfterCreate(
            key: String,
            value: CimdResolution,
            currentTime: Long,
          ): Long {
            val seconds =
              when (value) {
                is CimdResolution.Resolved -> positiveTtlSeconds
                CimdResolution.Withdrawn -> rejectedTtlSeconds
                CimdResolution.Rejected -> rejectedTtlSeconds
                CimdResolution.Unavailable -> unavailableTtlSeconds
              }
            return TimeUnit.SECONDS.toNanos(seconds)
          }

          override fun expireAfterUpdate(
            key: String,
            value: CimdResolution,
            currentTime: Long,
            currentDuration: Long,
          ): Long = currentDuration

          override fun expireAfterRead(
            key: String,
            value: CimdResolution,
            currentTime: Long,
            currentDuration: Long,
          ): Long = currentDuration
        },
      ).build()

  private fun weigh(value: CimdResolution): Int {
    val resolved = value as? CimdResolution.Resolved ?: return 1
    val client = resolved.client.client
    return 1 + client.clientId.length + client.name.length + client.redirectUris.sumOf { it.length }
  }

  internal fun estimatedSize(): Long {
    cache.cleanUp()
    return cache.estimatedSize()
  }

  companion object {
    const val MAX_ENTRIES = 10_000L
    const val AVERAGE_ENTRY_WEIGHT = 256L
    const val POSITIVE_TTL_SECONDS = 300L

    /** As long as a positive answer: a caller who can fill the fetch budget must not age a retirement out. */
    const val REJECTED_TTL_SECONDS = 300L
    const val UNAVAILABLE_TTL_SECONDS = 5L
  }
}
