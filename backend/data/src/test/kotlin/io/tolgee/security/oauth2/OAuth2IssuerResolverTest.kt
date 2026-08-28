package io.tolgee.security.oauth2

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.assertj.core.api.Assertions.assertThat
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
    assertThat(resolverFor(backEnd = null, frontEnd = null).configuredBaseUrl).isNull()
  }

  @Test
  fun `treats a blank url as unconfigured`() {
    assertThat(resolverFor(backEnd = "", frontEnd = "   ").configuredBaseUrl).isNull()
  }

  @Test
  fun `prefers the back-end url over the front-end url`() {
    assertThat(
      resolverFor(backEnd = "https://back.example.com", frontEnd = "https://front.example.com").configuredBaseUrl,
    ).isEqualTo("https://back.example.com")
  }

  @Test
  fun `falls back to the front-end url for single-origin deployments`() {
    assertThat(resolverFor(backEnd = null, frontEnd = "https://front.example.com").configuredBaseUrl)
      .isEqualTo("https://front.example.com")
  }

  @Test
  fun `strips a trailing slash so every consumer advertises the same issuer`() {
    assertThat(resolverFor(backEnd = "https://back.example.com/", frontEnd = null).configuredBaseUrl)
      .isEqualTo("https://back.example.com")
  }

  @Test
  fun `issuerUrl derives the origin from the request when nothing is configured`() {
    withRequest {
      assertThat(resolverFor(backEnd = null, frontEnd = null).issuerUrl).isEqualTo("https://tolgee.example.com")
    }
  }

  @Test
  fun `issuerUrl prefers configuration over the request`() {
    // Behind a reverse proxy the request's own origin is the internal one, so a configured URL has to win.
    withRequest {
      assertThat(resolverFor(backEnd = "https://public.example.com", frontEnd = null).issuerUrl)
        .isEqualTo("https://public.example.com")
    }
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
