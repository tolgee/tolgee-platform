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
import org.springframework.stereotype.Component
import java.util.Optional
import java.util.concurrent.TimeUnit

/**
 * A per-pod cache of CIMD clients resolved from their `client_id` URL.
 *
 * The keys are attacker-chosen, so the cache is bounded (LRU eviction at [maxSize]) and every fetch is single-flight:
 * [Cache.get] runs the loader at most once per key, so N concurrent cold authorizes for one `client_id` trigger one
 * fetch, not N. A resolved client is held [positiveTtlSeconds]; a failed resolution is cached (as an empty Optional)
 * for the shorter [negativeTtlSeconds], so a hostile URL cannot be replayed into an unbounded fetch storm.
 */
@Component
class CimdClientCache(
  private val metadataFetcher: CimdMetadataFetcher,
  maxSize: Long = MAX_ENTRIES,
  positiveTtlSeconds: Long = POSITIVE_TTL_SECONDS,
  negativeTtlSeconds: Long = NEGATIVE_TTL_SECONDS,
) {
  private val cache: Cache<String, Optional<CimdClient>> =
    Caffeine
      .newBuilder()
      .maximumSize(maxSize)
      .expireAfter(
        object : Expiry<String, Optional<CimdClient>> {
          override fun expireAfterCreate(
            key: String,
            value: Optional<CimdClient>,
            currentTime: Long,
          ): Long = TimeUnit.SECONDS.toNanos(if (value.isPresent) positiveTtlSeconds else negativeTtlSeconds)

          override fun expireAfterUpdate(
            key: String,
            value: Optional<CimdClient>,
            currentTime: Long,
            currentDuration: Long,
          ): Long = currentDuration

          override fun expireAfterRead(
            key: String,
            value: Optional<CimdClient>,
            currentTime: Long,
            currentDuration: Long,
          ): Long = currentDuration
        },
      ).build()

  fun get(clientIdUrl: String): CimdClient? =
    cache.get(clientIdUrl) { Optional.ofNullable(metadataFetcher.fetchAndValidate(it)) }.orElse(null)

  internal fun estimatedSize(): Long {
    cache.cleanUp()
    return cache.estimatedSize()
  }

  companion object {
    const val MAX_ENTRIES = 10_000L
    const val POSITIVE_TTL_SECONDS = 300L
    const val NEGATIVE_TTL_SECONDS = 60L
  }
}
