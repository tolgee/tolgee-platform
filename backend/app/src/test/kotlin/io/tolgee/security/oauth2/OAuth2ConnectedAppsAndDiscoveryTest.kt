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

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class OAuth2ConnectedAppsAndDiscoveryTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Test
  fun `lists no connected apps for a user who has authorized none`() {
    performAuthGet("/v2/user/connected-apps")
      .andIsOk
      .andAssertThatJson { isArray.hasSize(0) }
  }

  @Test
  fun `omits a grant whose registered client can no longer be resolved`() {
    // A removed pre-registered client leaves grants pointing at a client that no longer resolves; list() drops the
    // unresolvable row (the grant is still killable via logout-everywhere).
    val principal = userAccount!!.id.toString()
    jdbcTemplate.update(
      "INSERT INTO oauth2_authorization " +
        "(id, registered_client_id, principal_name, authorization_grant_type, access_token_value) " +
        "VALUES (?, ?, ?, 'authorization_code', 'token')",
      "orphan-authz",
      "nonexistent-client",
      principal,
    )
    try {
      performAuthGet("/v2/user/connected-apps")
        .andIsOk
        .andAssertThatJson { isArray.hasSize(0) }
    } finally {
      jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", "orphan-authz")
    }
  }

  @Test
  fun `serves RFC 9728 protected resource metadata for the MCP endpoint`() {
    performGet("/.well-known/oauth-protected-resource/mcp/developer")
      .andIsOk
      .andAssertThatJson {
        node("resource").isString
        node("authorization_servers").isArray
        node("scopes_supported").isArray.contains("translations.suggest")
      }
  }
}
