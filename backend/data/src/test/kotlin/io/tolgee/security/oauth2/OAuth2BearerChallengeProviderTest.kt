package io.tolgee.security.oauth2

import io.tolgee.mcp.McpConstants
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest

class OAuth2BearerChallengeProviderTest {
  @Test
  fun `an unusable issuer costs the challenge its metadata pointer, not the response`() {
    val provider =
      OAuth2BearerChallengeProvider(
        mock {
          on { isConfigured } doReturn true
          on { issuerUrl } doThrow IllegalStateException("bad issuer")
        },
      )

    val challenge = provider.challengeFor(mcpRequest(), HttpStatus.UNAUTHORIZED)

    challenge.assert.isEqualTo("Bearer")
  }

  @Test
  fun `a usable issuer points at the protected-resource document`() {
    val provider =
      OAuth2BearerChallengeProvider(
        mock {
          on { isConfigured } doReturn true
          on { issuerUrl } doReturn "https://tolgee.example.com"
        },
      )

    val challenge = provider.challengeFor(mcpRequest(), HttpStatus.UNAUTHORIZED)

    challenge.assert
      .isNotNull()
      .contains("resource_metadata=\"https://tolgee.example.com${OAuth2Constants.PROTECTED_RESOURCE_METADATA_PATH}\"")
  }

  @Test
  fun `a deployment that publishes no protected-resource document is not pointed at one`() {
    val provider = OAuth2BearerChallengeProvider(mock { on { isConfigured } doReturn false })

    provider.challengeFor(mcpRequest(), HttpStatus.UNAUTHORIZED).assert.isEqualTo("Bearer")
  }

  private fun mcpRequest() = MockHttpServletRequest("POST", McpConstants.DEVELOPER_ENDPOINT_PATH)
}
