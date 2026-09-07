package io.tolgee.security.oauth2

import io.tolgee.security.oauth2.OAuth2Audience
import io.tolgee.security.oauth2.OAuth2Error
import io.tolgee.security.oauth2.OAuth2IssuerResolver
import io.tolgee.security.oauth2.OAuth2Resources
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class OAuth2ResourcesTest {
  private val issuerResolver = mock<OAuth2IssuerResolver> { on { issuerUrl } doReturn "https://app.tolgee.io" }
  private val resources = OAuth2Resources(issuerResolver)

  @Test
  fun `absent resource defaults to the api audience`() {
    assertThat(resources.audienceFor(null)).isEqualTo(OAuth2Audience.API)
  }

  @Test
  fun `the issuer origin is the api audience`() {
    assertThat(resources.audienceFor("https://app.tolgee.io")).isEqualTo(OAuth2Audience.API)
  }

  @Test
  fun `the mcp canonical uri is the mcp audience`() {
    assertThat(resources.audienceFor("https://app.tolgee.io/mcp/developer")).isEqualTo(OAuth2Audience.MCP)
  }

  @Test
  fun `an unknown resource is invalid_target`() {
    assertThatThrownBy { resources.audienceFor("https://evil.example/mcp") }
      .isInstanceOf(OAuth2Error::class.java)
      .hasFieldOrPropertyWithValue("error", OAuth2Error.INVALID_TARGET)
  }

  @Test
  fun `a trailing slash does not match`() {
    assertThatThrownBy { resources.audienceFor("https://app.tolgee.io/") }
      .isInstanceOf(OAuth2Error::class.java)
  }
}
