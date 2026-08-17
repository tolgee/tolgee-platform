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
import io.tolgee.testing.AbstractControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/**
 * Boots the full application context to verify the OAuth2 authorization-server filter chain coexists with the main
 * stateless chain, and that the discovery + JWKS endpoints (which prove the asymmetric keys and settings are wired)
 * are served.
 */
class OAuth2AuthorizationServerTest : AbstractControllerTest() {
  @Test
  fun `serves the JWKS with an RSA public key`() {
    val body =
      mvc
        .perform(get("/oauth2/jwks"))
        .andIsOk
        .andReturn()
        .response.contentAsString
    assertThat(body).contains("\"kty\":\"RSA\"")
    assertThat(body).doesNotContain("\"d\":")
  }

  @Test
  fun `serves the authorization server metadata`() {
    mvc
      .perform(get("/.well-known/oauth-authorization-server"))
      .andIsOk
      .andAssertThatJson {
        node("authorization_endpoint").isString
        node("token_endpoint").isString
        node("jwks_uri").isString
        // CIMD support is advertised so spec-aware clients know they can self-register with a URL-form client_id
        node("client_id_metadata_document_supported").isEqualTo(true)
      }
  }
}
