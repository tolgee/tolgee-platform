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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.ApplicationArguments
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository

/**
 * The seeding runner has real conditional logic (skip a client with no redirect URIs) and security-load-bearing
 * per-client settings (PKCE required; consent required for the extension, not the CLI) that no integration test hits —
 * OAuth2AuthorizationCodeFlowTest registers its own clients instead.
 */
class PreRegisteredClientsTest {
  private val args = mock<ApplicationArguments>()

  @Test
  fun `seeds nothing when no redirect URIs are configured`() {
    val repo = mock<RegisteredClientRepository>()
    val properties =
      OAuth2ServerProperties().apply {
        browserExtensionRedirectUris = listOf()
        cliRedirectUris = listOf()
      }

    PreRegisteredClients(repo, properties).run(args)

    verify(repo, never()).save(any())
  }

  @Test
  fun `seeds the extension and CLI clients with PKCE and per-client consent when redirect URIs are configured`() {
    val repo = mock<RegisteredClientRepository>()
    val properties =
      OAuth2ServerProperties().apply {
        browserExtensionRedirectUris = listOf("https://ext.example/callback")
        cliRedirectUris = listOf("http://127.0.0.1:9876/callback")
      }

    PreRegisteredClients(repo, properties).run(args)

    val captor = argumentCaptor<RegisteredClient>()
    verify(repo, times(2)).save(captor.capture())
    val byClientId = captor.allValues.associateBy { it.clientId }

    val extension = byClientId.getValue(OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID)
    assertThat(extension.redirectUris).containsExactly("https://ext.example/callback")
    assertThat(extension.clientSettings.isRequireProofKey).isTrue()
    assertThat(extension.clientSettings.isRequireAuthorizationConsent).isTrue()

    val cli = byClientId.getValue(OAuth2Constants.CLI_CLIENT_ID)
    assertThat(cli.clientSettings.isRequireProofKey).isTrue()
    // Loopback public client: consent is required so a local process can't silently obtain a full-scope token.
    assertThat(cli.clientSettings.isRequireAuthorizationConsent).isTrue()
  }
}
