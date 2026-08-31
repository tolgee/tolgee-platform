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
    // Why propagating here would be worse is on OAuth2BearerChallengeProvider.resourceMetadataUrl.
    val provider =
      OAuth2BearerChallengeProvider(
        mock { on { issuerUrl } doThrow IllegalStateException("bad issuer") },
        registryWith(anyClient()),
      )

    val challenge = provider.challengeFor(mcpRequest(), HttpStatus.UNAUTHORIZED)

    challenge.assert.isEqualTo("Bearer")
  }

  @Test
  fun `a usable issuer points at the protected-resource document`() {
    val provider =
      OAuth2BearerChallengeProvider(
        mock { on { issuerUrl } doReturn "https://tolgee.example.com" },
        registryWith(anyClient()),
      )

    val challenge = provider.challengeFor(mcpRequest(), HttpStatus.UNAUTHORIZED)

    challenge.assert
      .isNotNull()
      .contains("resource_metadata=\"https://tolgee.example.com${OAuth2Constants.PROTECTED_RESOURCE_METADATA_PATH}\"")
  }

  @Test
  fun `a deployment that publishes no protected-resource document is not pointed at one`() {
    val provider =
      OAuth2BearerChallengeProvider(
        mock { on { issuerUrl } doReturn "https://tolgee.example.com" },
        registryWith(listOf()),
      )

    provider.challengeFor(mcpRequest(), HttpStatus.UNAUTHORIZED).assert.isEqualTo("Bearer")
  }

  private fun mcpRequest() = MockHttpServletRequest("POST", OAuth2Constants.MCP_RESOURCE_PATH)

  private fun anyClient() =
    listOf(OAuth2Client(clientId = "c", name = "c", redirectUris = listOf("https://ext.example/cb")))

  private fun registryWith(clients: List<OAuth2Client>): OAuth2ClientRegistry =
    mock { on { isEnabled } doReturn clients.isNotEmpty() }
}
