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

import io.tolgee.constants.Caches
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Whether an oauth2_authorization row still exists — the "is this grant alive?" check the resolver runs on every OAuth
 * request. Keyed by authorization id (not token value): one authorization owns many tokens over its life as refresh
 * rotates them, so keying by id keeps the whole family as a single cache entry. Revoking a grant (disconnect,
 * logout-everywhere) deletes the row and [evict]s the entry, which is what makes revocation take effect on the next
 * request instead of at token expiry. A missing row caches as `false` and fails closed.
 */
@Service
class OAuth2AuthorizationLivenessService(
  private val repository: OAuth2AuthorizationJdbcRepository,
  private val cacheManager: CacheManager,
) {
  @Cacheable(cacheNames = [Caches.OAUTH2_AUTHORIZATIONS], key = "#authorizationId")
  fun isLive(authorizationId: String): Boolean = repository.existsById(authorizationId)

  fun evict(authorizationId: String) {
    cacheManager.getCache(Caches.OAUTH2_AUTHORIZATIONS)?.evict(authorizationId)
  }
}
