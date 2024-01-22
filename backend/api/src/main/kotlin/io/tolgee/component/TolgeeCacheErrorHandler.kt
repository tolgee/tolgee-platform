/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
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

package io.tolgee.component

import org.redisson.client.RedisException
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.SimpleCacheErrorHandler

class TolgeeCacheErrorHandler : SimpleCacheErrorHandler() {
  private val logger = LoggerFactory.getLogger(TolgeeCacheErrorHandler::class.java)

  override fun handleCacheGetError(
    exception: RuntimeException,
    cache: Cache,
    key: Any,
  ) {
    if (exception is RedisException) {
      logger.warn(
        "Suppressing RedisException for cache {} on key {}. " +
          "This is likely due to outdated cache data, therefore this cache entry has been removed. " +
          "If this re-occurs for the same cache and key, it is likely from a bug that should be reported.",
        cache.name,
        key,
      )

      logger.warn("The following error occurred while fetching the key", exception)

      cache.evictIfPresent(key)
      return
    }

    throw exception
  }
}
