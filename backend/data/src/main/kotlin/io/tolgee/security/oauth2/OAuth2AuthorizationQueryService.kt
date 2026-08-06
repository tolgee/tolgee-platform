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

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant

/**
 * JDBC queries over the Spring Authorization Server tables that its services don't expose (a user's authorized clients;
 * revoking every grant/consent for one client). `principal_name` is the Tolgee user id — see the flow's session
 * bootstrap — so callers pass `user.id.toString()`.
 */
@Service
class OAuth2AuthorizationQueryService(
  private val jdbcTemplate: JdbcTemplate,
) {
  fun findAuthorizedClients(principalName: String): List<AuthorizedClient> {
    // Consent-required clients (browser extension, CIMD) get an oauth2_authorization row at /authorize BEFORE the user
    // approves; filtering on an issued token keeps abandoned/unapproved consent rows out of the connected-apps list.
    return jdbcTemplate.query(
      """
      SELECT registered_client_id, MAX(access_token_issued_at) AS last_authorized_at
      FROM oauth2_authorization
      WHERE principal_name = ? AND (access_token_value IS NOT NULL OR refresh_token_value IS NOT NULL)
      GROUP BY registered_client_id
      """.trimIndent(),
      { rs, _ ->
        AuthorizedClient(
          registeredClientId = rs.getString("registered_client_id"),
          lastAuthorizedAt = rs.getTimestamp("last_authorized_at")?.toInstant(),
        )
      },
      principalName,
    )
  }

  /** Deletes the user's authorizations and consents for the client; returns the authorization-row (not consent) count. */
  fun revoke(
    registeredClientId: String,
    principalName: String,
  ): Int {
    jdbcTemplate.update(
      "DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ? AND principal_name = ?",
      registeredClientId,
      principalName,
    )
    return jdbcTemplate.update(
      "DELETE FROM oauth2_authorization WHERE registered_client_id = ? AND principal_name = ?",
      registeredClientId,
      principalName,
    )
  }

  /**
   * Deletes authorizations before [cutoff]: those whose newest credential expiry (refresh, then access, then code) has
   * passed — so a still-valid refresh token is never deleted — plus abandoned pre-consent rows (no token/code at all,
   * every expiry NULL) that were created before the cutoff, which the expiry predicate alone can never match.
   */
  fun deleteExpiredBefore(cutoff: Instant): Int {
    val ts = Timestamp.from(cutoff)
    return jdbcTemplate.update(
      """
      DELETE FROM oauth2_authorization
      WHERE COALESCE(refresh_token_expires_at, access_token_expires_at, authorization_code_expires_at) < ?
         OR (refresh_token_expires_at IS NULL AND access_token_expires_at IS NULL
             AND authorization_code_expires_at IS NULL AND created_at < ?)
      """.trimIndent(),
      ts,
      ts,
    )
  }

  data class AuthorizedClient(
    val registeredClientId: String,
    val lastAuthorizedAt: Instant?,
  )
}
