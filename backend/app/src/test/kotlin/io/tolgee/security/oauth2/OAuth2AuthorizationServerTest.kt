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
 * stateless chain, and that the discovery endpoint is served.
 */
class OAuth2AuthorizationServerTest : AbstractControllerTest() {
  @Test
  fun `sets a referrer policy on the authorization endpoint`() {
    // /oauth2/authorize answers with redirects and error pages whose URLs carry `code` and `state`. Spring sets no
    // Referrer-Policy by default, and this chain does not inherit the main chain's headers, so it must set its own.
    val response = mvc.perform(get("/oauth2/authorize")).andReturn().response
    assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin")
  }

  @Test
  fun `publishes no JWK set`() {
    // Access tokens are opaque, so the server holds no signing key and Spring registers no JWK-set endpoint. If this
    // ever starts returning a key set, a key lifecycle has been reintroduced without anything needing one.
    assertThat(
      mvc
        .perform(get("/oauth2/jwks"))
        .andReturn()
        .response.status,
    ).isNotEqualTo(200)
  }

  @Test
  fun `serves the authorization server metadata`() {
    mvc
      .perform(get("/.well-known/oauth-authorization-server"))
      .andIsOk
      .andAssertThatJson {
        node("authorization_endpoint").isString
        node("token_endpoint").isString
        // No JWK set is published, so discovery must not advertise a jwks_uri that would 404.
        node("jwks_uri").isAbsent()
      }
  }
}
