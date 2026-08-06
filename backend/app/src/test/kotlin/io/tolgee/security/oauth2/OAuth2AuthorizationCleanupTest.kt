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

import io.tolgee.AbstractSpringTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant

class OAuth2AuthorizationCleanupTest : AbstractSpringTest() {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var queryService: OAuth2AuthorizationQueryService

  @AfterEach
  fun cleanup() {
    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE registered_client_id = 'cleanup-test-client'")
  }

  @Test
  fun `deletes only authorizations whose credentials all expired before the cutoff`() {
    val now = Instant.now()
    val old = now.minus(Duration.ofDays(10))
    val recent = now.minus(Duration.ofDays(2))
    val future = now.plus(Duration.ofDays(20))

    insert("cleanup-old", refreshExpiresAt = old)
    insert("cleanup-fresh", refreshExpiresAt = future)
    insert("cleanup-recent", refreshExpiresAt = recent)
    insert("cleanup-live-refresh", refreshExpiresAt = future, accessExpiresAt = old, codeExpiresAt = old)
    insert("cleanup-access-only", refreshExpiresAt = null, accessExpiresAt = old, codeExpiresAt = null)

    val deleted = queryService.deleteExpiredBefore(now.minus(Duration.ofDays(7)))

    assertThat(deleted).isEqualTo(2)
    assertThat(exists("cleanup-old")).isFalse()
    assertThat(exists("cleanup-access-only")).isFalse()
    assertThat(exists("cleanup-fresh")).isTrue()
    assertThat(exists("cleanup-recent")).isTrue()
    assertThat(exists("cleanup-live-refresh")).isTrue()
  }

  @Test
  fun `deletes abandoned pre-consent rows (all expiries NULL) older than the cutoff but keeps in-flight ones`() {
    val now = Instant.now()
    insert("cleanup-pending-old", refreshExpiresAt = null, createdAt = now.minus(Duration.ofDays(10)))
    insert("cleanup-pending-fresh", refreshExpiresAt = null, createdAt = now)

    queryService.deleteExpiredBefore(now.minus(Duration.ofDays(7)))

    assertThat(exists("cleanup-pending-old")).isFalse()
    assertThat(exists("cleanup-pending-fresh")).isTrue()
  }

  private fun insert(
    id: String,
    refreshExpiresAt: Instant?,
    accessExpiresAt: Instant? = null,
    codeExpiresAt: Instant? = null,
    createdAt: Instant? = null,
  ) {
    jdbcTemplate.update(
      """
      INSERT INTO oauth2_authorization
        (id, registered_client_id, principal_name, authorization_grant_type,
         refresh_token_expires_at, access_token_expires_at, authorization_code_expires_at, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP))
      """.trimIndent(),
      id,
      "cleanup-test-client",
      "1",
      "authorization_code",
      refreshExpiresAt?.let { Timestamp.from(it) },
      accessExpiresAt?.let { Timestamp.from(it) },
      codeExpiresAt?.let { Timestamp.from(it) },
      createdAt?.let { Timestamp.from(it) },
    )
  }

  private fun exists(id: String): Boolean {
    val count =
      jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM oauth2_authorization WHERE id = ?",
        Int::class.java,
        id,
      )
    return (count ?: 0) > 0
  }
}
