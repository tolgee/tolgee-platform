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

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class OAuth2IssuerResolverTest {
  @Test
  fun `a blank url counts as unset, so reading the issuer fails`() {
    assertThatThrownBy { resolverFor(backEnd = "", frontEnd = "   ").issuerUrl }
      .isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `prefers the back-end url over the front-end url`() {
    resolverFor(
      backEnd = "https://back.example.com",
      frontEnd = "https://front.example.com",
    ).issuerUrl.assert.isEqualTo("https://back.example.com")
  }

  @Test
  fun `falls back to the front-end url for single-origin deployments`() {
    resolverFor(backEnd = null, frontEnd = "https://front.example.com")
      .issuerUrl.assert
      .isEqualTo("https://front.example.com")
  }

  @Test
  fun `strips a trailing slash so every consumer advertises the same issuer`() {
    resolverFor(backEnd = "https://back.example.com/", frontEnd = null)
      .issuerUrl.assert
      .isEqualTo("https://back.example.com")
  }

  @Test
  fun `issuerUrl refuses to invent an issuer when nothing is configured`() {
    assertThatThrownBy { resolverFor(backEnd = null, frontEnd = null).issuerUrl }
      .isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `issuerUrl is the configured value`() {
    resolverFor(backEnd = "https://public.example.com", frontEnd = null)
      .issuerUrl.assert
      .isEqualTo("https://public.example.com")
  }

  @Test
  fun `refuses an issuer with a path, naming the property to fix`() {
    // RFC 8414 §3 would put the metadata document at a path Tolgee does not serve, so this must fail loudly at the
    // first read rather than dead-ending the flow in the browser.
    assertThatThrownBy { resolverFor("https://tolgee.example.com/tolgee", null).issuerUrl }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("tolgee.back-end-url")

    assertThatThrownBy { resolverFor(null, "https://tolgee.example.com/app").issuerUrl }
      .isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `refuses an issuer carrying a query or a fragment`() {
    // RFC 8414 §2: the issuer identifier has no query or fragment components.
    assertThatThrownBy { resolverFor("https://tolgee.example.com?tenant=a", null).issuerUrl }
      .isInstanceOf(IllegalStateException::class.java)
    assertThatThrownBy { resolverFor("https://tolgee.example.com#f", null).issuerUrl }
      .isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `accepts a bare origin, with or without a trailing slash`() {
    resolverFor("https://tolgee.example.com/", null).issuerUrl.assert.isEqualTo("https://tolgee.example.com")
    resolverFor("https://tolgee.example.com", null).issuerUrl.assert.isEqualTo("https://tolgee.example.com")
  }

  private fun resolverFor(
    backEnd: String?,
    frontEnd: String?,
    clients: List<OAuth2Client> = listOf(),
  ) = OAuth2IssuerResolver(
    mock<TolgeeProperties> {
      on { backEndUrl } doReturn backEnd
      on { frontEndUrl } doReturn frontEnd
    },
    mock<OAuth2ClientRegistry> { on { isEnabled } doReturn clients.isNotEmpty() },
  )

  private fun anyClient() =
    listOf(OAuth2Client(clientId = "c", name = "c", redirectUris = listOf("https://ext.example/cb")))

  @Test
  fun `an unusable issuer fails startup once a client is configured`() {
    assertThatThrownBy {
      resolverFor("https://tolgee.example.com/tolgee", null, anyClient()).requireConfiguredIssuer()
    }.isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `an unset issuer fails startup once a client is configured`() {
    assertThatThrownBy {
      resolverFor(null, null, anyClient()).requireConfiguredIssuer()
    }.isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `an unusable issuer does not fail startup for a deployment that issues no tokens`() {
    assertThatCode {
      resolverFor("https://tolgee.example.com/tolgee", null).requireConfiguredIssuer()
    }.doesNotThrowAnyException()
  }
}
