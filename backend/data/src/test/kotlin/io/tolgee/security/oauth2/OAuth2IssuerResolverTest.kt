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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class OAuth2IssuerResolverTest {
  @Test
  fun `has no issuer when no base url is configured`() {
    // Callers derive one from the request instead; there is no sensible literal, and a non-URL here would be published
    // in discovery documents where clients dereference it.
    resolverFor(backEnd = null, frontEnd = null).configuredBaseUrl.assert.isNull()
  }

  @Test
  fun `treats a blank url as unconfigured`() {
    resolverFor(backEnd = "", frontEnd = "   ").configuredBaseUrl.assert.isNull()
  }

  @Test
  fun `prefers the back-end url over the front-end url`() {
    resolverFor(
      backEnd = "https://back.example.com",
      frontEnd = "https://front.example.com",
    ).configuredBaseUrl.assert.isEqualTo("https://back.example.com")
  }

  @Test
  fun `falls back to the front-end url for single-origin deployments`() {
    resolverFor(backEnd = null, frontEnd = "https://front.example.com")
      .configuredBaseUrl.assert
      .isEqualTo("https://front.example.com")
  }

  @Test
  fun `strips a trailing slash so every consumer advertises the same issuer`() {
    resolverFor(backEnd = "https://back.example.com/", frontEnd = null)
      .configuredBaseUrl.assert
      .isEqualTo("https://back.example.com")
  }

  @Test
  fun `issuerUrl derives the origin from the request when nothing is configured`() {
    withRequest {
      resolverFor(backEnd = null, frontEnd = null).issuerUrl.assert.isEqualTo("https://tolgee.example.com")
    }
  }

  @Test
  fun `issuerUrl prefers configuration over the request`() {
    // Behind a reverse proxy the request's own origin is the internal one, so a configured URL has to win.
    withRequest {
      resolverFor(backEnd = "https://public.example.com", frontEnd = null)
        .issuerUrl.assert
        .isEqualTo("https://public.example.com")
    }
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
  fun `accepts a bare origin, with or without a trailing slash`() {
    resolverFor("https://tolgee.example.com/", null).issuerUrl.assert.isEqualTo("https://tolgee.example.com")
    resolverFor("https://tolgee.example.com", null).issuerUrl.assert.isEqualTo("https://tolgee.example.com")
  }

  private fun withRequest(body: () -> Unit) {
    val request =
      MockHttpServletRequest("GET", "/oauth2/authorize").apply {
        scheme = "https"
        serverName = "tolgee.example.com"
        serverPort = 443
      }
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    try {
      body()
    } finally {
      RequestContextHolder.resetRequestAttributes()
    }
  }

  private fun resolverFor(
    backEnd: String?,
    frontEnd: String?,
  ) = OAuth2IssuerResolver(
    mock<TolgeeProperties> {
      on { backEndUrl } doReturn backEnd
      on { frontEndUrl } doReturn frontEnd
    },
  )
}
