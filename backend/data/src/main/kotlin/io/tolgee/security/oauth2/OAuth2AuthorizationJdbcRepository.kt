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
  fun deleteConsentByPrincipal(principalName: String): Int =
    jdbcTemplate.update("DELETE FROM oauth2_authorization_consent WHERE principal_name = ?", principalName)

  fun deleteByPrincipal(principalName: String): Int =
    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE principal_name = ?", principalName)

  fun deleteById(id: String): Int = jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", id)

  // COALESCE takes the newest expiry, so a row with a still-valid refresh token survives a long-expired access token.
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
}
