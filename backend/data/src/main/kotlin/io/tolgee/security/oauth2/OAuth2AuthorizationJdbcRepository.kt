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
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Repository
class OAuth2AuthorizationJdbcRepository(
  private val jdbcTemplate: JdbcTemplate,
) {
  // SAS's JdbcRegisteredClientRepository has no delete, so honoring "disable a pre-registered client by emptying its
  // redirect config" (rather than leaving a stale full-scope row usable) needs this direct delete.
  fun deleteRegisteredClient(clientId: String): Int =
    jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", clientId)

  fun findAuthorizedClients(principalName: String): List<AuthorizedClient> {
    // A row exists at /authorize before consent; the token-value filter keeps abandoned pre-consent rows off the list.
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

  fun existsById(id: String): Boolean {
    val count =
      jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oauth2_authorization WHERE id = ?", Int::class.java, id) ?: 0
    return count > 0
  }

  fun findIdsByClientAndPrincipal(
    registeredClientId: String,
    principalName: String,
  ): List<String> =
    jdbcTemplate
      .queryForList(
        "SELECT id FROM oauth2_authorization WHERE registered_client_id = ? AND principal_name = ?",
        String::class.java,
        registeredClientId,
        principalName,
      ).filterNotNull()

  fun findIdsByPrincipal(principalName: String): List<String> =
    jdbcTemplate
      .queryForList("SELECT id FROM oauth2_authorization WHERE principal_name = ?", String::class.java, principalName)
      .filterNotNull()

  fun deleteConsentByClientAndPrincipal(
    registeredClientId: String,
    principalName: String,
  ): Int =
    jdbcTemplate.update(
      "DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ? AND principal_name = ?",
      registeredClientId,
      principalName,
    )

  fun deleteConsentByPrincipal(principalName: String): Int =
    jdbcTemplate.update("DELETE FROM oauth2_authorization_consent WHERE principal_name = ?", principalName)

  fun deleteByClientAndPrincipal(
    registeredClientId: String,
    principalName: String,
  ): Int =
    jdbcTemplate.update(
      "DELETE FROM oauth2_authorization WHERE registered_client_id = ? AND principal_name = ?",
      registeredClientId,
      principalName,
    )

  fun deleteByPrincipal(principalName: String): Int =
    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE principal_name = ?", principalName)

  fun deleteById(id: String): Int = jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", id)

  // Deletes rows whose newest credential expiry (refresh > access > code) passed — so a still-valid refresh token is
  // never deleted — plus abandoned pre-consent rows (all expiries NULL) before [cutoff], which the expiry test misses.
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
