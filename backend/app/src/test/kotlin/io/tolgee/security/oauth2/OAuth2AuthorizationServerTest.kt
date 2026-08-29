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
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options

/** Boots the full application context to verify the OAuth2 endpoints are served under the main security chain. */
class OAuth2AuthorizationServerTest : AbstractControllerTest() {
  private fun corsHeader(path: String): String? =
    mvc
      .perform(options(path).header("Origin", "https://ext.example").header("Access-Control-Request-Method", "GET"))
      .andReturn()
      .response
      .getHeader("Access-Control-Allow-Origin")

  @Test
  fun `sets a referrer policy on the authorization endpoint`() {
    // /oauth2/authorize redirects to URLs carrying `code` and `state`, so the referrer must not leak them.
    val response = mvc.perform(get("/oauth2/authorize")).andReturn().response
    response.getHeader("Referrer-Policy").assert.isEqualTo("strict-origin-when-cross-origin")
  }

  @Test
  fun `CORS is offered on the token and discovery endpoints but not on authorize`() {
    corsHeader("/oauth2/authorize").assert.isNull()
    corsHeader("/oauth2/token").assert.isEqualTo("*")
    corsHeader("/.well-known/oauth-authorization-server").assert.isEqualTo("*")
  }

  @Test
  fun `publishes no JWK set`() {
    // Access tokens are opaque, so the server holds no signing key. If this ever starts answering with a key set, a
    // key lifecycle has been reintroduced without anything needing one. (An unknown path is answered by the SPA
    // catch-all, so the check is on the content, not the status.)
    val response = mvc.perform(get("/oauth2/jwks")).andReturn().response
    (response.contentType ?: "").assert.doesNotContain("json")
    response.contentAsString.assert.doesNotContain("\"keys\"")
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
