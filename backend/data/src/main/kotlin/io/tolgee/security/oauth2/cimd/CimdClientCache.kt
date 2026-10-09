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
import io.tolgee.Metrics
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * The per-pod memory of resolved `client_id` documents for the request path. Only a resolved client is kept: the
 * CIMD draft (section 5.2) forbids caching error responses and invalid documents, and nothing here needs them. A
 * negative answer is given to the callers waiting on that fetch and then forgotten, so the next request asks the
 * publisher again, within the bounds [CimdFetchBudget] sets.
 */
@Component
class CimdClientCache(
  private val metadataFetcher: CimdMetadataFetcher,
  private val metrics: Metrics,
  private val budget: CimdFetchBudget,
  approximateMaxEntries: Long = MAX_ENTRIES,
  positiveTtlSeconds: Long = POSITIVE_TTL_SECONDS,
  ticker: Ticker = Ticker.systemTicker(),
) {
  // The value type is nullable only so the loader may answer null, which Caffeine then does not store.
  private val cache: Cache<String, CimdClient?> = buildCache(approximateMaxEntries, positiveTtlSeconds, ticker)

  /**
   * A miss takes a budget slot before entering [Cache.get], never inside it — see [CimdFetchBudget] for why. The
   * wrapper tells a budget refusal apart from a document that resolved to nothing.
   */
  fun get(clientIdUrl: String): CimdClient? {
    cache.getIfPresent(clientIdUrl)?.let { return it }
    val loaded = budget.withBudget(clientIdUrl) { Loaded(loadResolved(clientIdUrl)) } ?: return refuseForNoCapacity()
    return loaded.client
  }

  /**
   * Fetches the publisher's document now on the [CimdFetchLane.GRANT_CHECK] lane, with no cache on either side of
   * the call. Null means this server had no room for the fetch and the publisher was never asked.
   */
  fun fetchOnGrantLane(clientIdUrl: String): CimdResolution? =
    try {
      metadataFetcher.fetchAndValidate(clientIdUrl, CimdFetchLane.GRANT_CHECK)
    } catch (_: CimdNoCapacityException) {
      metrics.oauth2CimdCapacityRefusalsCounter.increment()
      null
    }

  /** What is already known, with no outbound call: for callers that run on every request. */
  fun cachedClient(clientIdUrl: String): CimdClient? = cache.getIfPresent(clientIdUrl)

  fun invalidate(clientIdUrl: String) {
    cache.invalidate(clientIdUrl)
  }

  /** Caffeine stores nothing when the loader answers null, which is what keeps every non-resolved answer out. */
  private fun loadResolved(clientIdUrl: String): CimdClient? =
    try {
      cache.get(clientIdUrl) { (metadataFetcher.fetchAndValidate(it) as? CimdResolution.Resolved)?.client }
    } catch (_: CimdNoCapacityException) {
      refuseForNoCapacity()
    }

  private fun refuseForNoCapacity(): CimdClient? {
    metrics.oauth2CimdCapacityRefusalsCounter.increment()
    return null
  }

  private class Loaded(
    val client: CimdClient?,
  )

  private fun buildCache(
    approximateMaxEntries: Long,
    positiveTtlSeconds: Long,
    ticker: Ticker,
  ): Cache<String, CimdClient?> =
    Caffeine
      .newBuilder()
      .maximumWeight(approximateMaxEntries * AVERAGE_ENTRY_WEIGHT)
      .weigher { key: String, value: CimdClient? -> key.length + weigh(value) }
      .ticker(ticker)
      .expireAfterWrite(positiveTtlSeconds, TimeUnit.SECONDS)
      .build()

  private fun weigh(value: CimdClient?): Int {
    val client = value?.client ?: return 1
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
  }
}
