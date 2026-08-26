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

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

// SAS stores the user id as `principal_name`, so callers pass `user.id.toString()`.
@Service
class OAuth2AuthorizationQueryService(
  private val repository: OAuth2AuthorizationJdbcRepository,
) {
  /** Deletes ALL of the user's authorizations and consents (logout-everywhere); returns the authorization-row count. */
  fun revokeAllForPrincipal(principalName: String): Int {
    repository.deleteConsentByPrincipal(principalName)
    return repository.deleteByPrincipal(principalName)
  }

  // Runs in its own transaction: the caller (the token customizer detecting an invalidated refresh grant) throws right
  // after, rolling back the refresh-grant transaction — the deletion must survive that rollback to actually revoke.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun revokeByIdInNewTransaction(authorizationId: String) {
    repository.deleteById(authorizationId)
  }

  fun deleteExpiredBefore(cutoff: Instant): Int = repository.deleteExpiredBefore(cutoff)
}
