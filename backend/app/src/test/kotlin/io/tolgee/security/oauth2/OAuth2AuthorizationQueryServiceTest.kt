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

/**
 * `principal_name` scoping is the only thing keeping a revoke from reaching another user's OAuth grants, so the
 * cross-user isolation of revokeAllForUser (irreversible cross-tenant deletion) is asserted.
 *
 * The consent half covers the upgrade case only — nothing writes `oauth2_authorization_consent` while
 * [io.tolgee.security.oauth2.AlwaysPromptConsentService] is the consent service.
 */
class OAuth2AuthorizationQueryServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var queryService: OAuth2AuthorizationQueryService

  private val userAId = 1001L
  private val userBId = 1002L
  private val userA = userAId.toString()
  private val userB = userBId.toString()

  @AfterEach
  fun cleanup() {
    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE principal_name IN (?, ?)", userA, userB)
    jdbcTemplate.update("DELETE FROM oauth2_authorization_consent WHERE principal_name IN (?, ?)", userA, userB)
  }

  @Test
  fun `revokeAllForUser deletes only the requesting user's grants and consents`() {
    // logout-everywhere / password change deletes by principal_name with no client filter; a wrong or missing WHERE
    // clause here would wipe every user's grants on any single user's invalidation.
    insertGrant("a-1", "client-x", userA)
    insertGrant("a-2", "client-y", userA)
    insertGrant("b-1", "client-x", userB)
    insertConsent("client-x", userA)
    insertConsent("client-x", userB)

    queryService.revokeAllForUser(userAId)

    assertThat(authorizationExists("a-1")).isFalse()
    assertThat(authorizationExists("a-2")).isFalse()
    assertThat(authorizationExists("b-1")).isTrue()
    assertThat(consentExists("client-x", userA)).isFalse()
    assertThat(consentExists("client-x", userB)).isTrue()
  }

  private fun insertGrant(
    id: String,
    clientId: String,
    principalName: String,
  ) {
    jdbcTemplate.update(
      """
      INSERT INTO oauth2_authorization
        (id, registered_client_id, principal_name, authorization_grant_type, access_token_value)
      VALUES (?, ?, ?, 'authorization_code', 'token')
      """.trimIndent(),
      id,
      clientId,
      principalName,
    )
  }

  private fun insertConsent(
    clientId: String,
    principalName: String,
  ) {
    jdbcTemplate.update(
      "INSERT INTO oauth2_authorization_consent (registered_client_id, principal_name, authorities) VALUES (?, ?, 'x')",
      clientId,
      principalName,
    )
  }

  private fun authorizationExists(id: String): Boolean =
    (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oauth2_authorization WHERE id = ?", Int::class.java, id) ?: 0) >
      0

  private fun consentExists(
    clientId: String,
    principalName: String,
  ): Boolean =
    (
      jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM oauth2_authorization_consent WHERE registered_client_id = ? AND principal_name = ?",
        Int::class.java,
        clientId,
        principalName,
      ) ?: 0
    ) > 0
}
