package io.tolgee.security.oauth2

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class OAuth2AudienceResolverTest {
  @Test
  fun `falls back to the default audience when no base url is configured`() {
    val properties =
      mock<TolgeeProperties> {
        on { backEndUrl } doReturn null
        on { frontEndUrl } doReturn null
      }
    assertThat(OAuth2AudienceResolver(properties).apiAudience).isEqualTo("tolgee-api")
  }

  @Test
  fun `prefers the back-end url over the front-end url`() {
    val properties =
      mock<TolgeeProperties> {
        on { backEndUrl } doReturn "https://back.example.com"
        on { frontEndUrl } doReturn "https://front.example.com"
      }
    val resolver = OAuth2AudienceResolver(properties)
    assertThat(resolver.serverBaseUrl).isEqualTo("https://back.example.com")
    assertThat(resolver.apiAudience).isEqualTo("https://back.example.com")
  }
}
