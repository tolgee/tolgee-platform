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

class OAuth2ProtectedResourceMetadataTest : AuthorizedControllerTest() {
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
