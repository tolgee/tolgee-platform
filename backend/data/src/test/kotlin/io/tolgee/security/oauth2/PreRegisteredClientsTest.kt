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

import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The client set has real conditional logic (skip a client with no redirect URIs) and security-load-bearing per-client
 * settings (PKCE required; consent required for both) that no integration test hits — OAuth2AuthorizationCodeFlowTest
 * registers its own clients instead.
 */
class PreRegisteredClientsTest {
  @Test
  fun `configures no clients when no redirect URIs are set`() {
    // Emptying the config is how an operator disables a client, and nothing is persisted, so it simply stops existing.
    val clients = clientsFor(extensionUris = listOf(), cliUris = listOf())

    assertThat(clients).isEmpty()
  }

  @Test
  fun `an empty client set is still a usable repository`() {
    // Spring's InMemoryRegisteredClientRepository rejects an empty list; ours must not, because no clients configured
    // is the default state and the application still has to start.
    val repository = TolgeeRegisteredClientRepository(preRegisteredClients(listOf(), listOf()))

    assertThat(repository.findByClientId(OAuth2Constants.CLI_CLIENT_ID)).isNull()
  }

  @Test
  fun `configures the extension and CLI clients with PKCE and consent when redirect URIs are set`() {
    val clients =
      clientsFor(
        extensionUris = listOf("https://ext.example/callback"),
        cliUris = listOf("http://127.0.0.1:9876/callback"),
      ).associateBy { it.clientId }

    val extension = clients.getValue(OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID)
    assertThat(extension.redirectUris).containsExactly("https://ext.example/callback")
    assertThat(extension.clientSettings.isRequireProofKey).isTrue()
    assertThat(extension.clientSettings.isRequireAuthorizationConsent).isTrue()

    val cli = clients.getValue(OAuth2Constants.CLI_CLIENT_ID)
    assertThat(cli.clientSettings.isRequireProofKey).isTrue()
    // Loopback public client: consent is required so a local process can't silently obtain a full-scope token.
    assertThat(cli.clientSettings.isRequireAuthorizationConsent).isTrue()
  }

  @Test
  fun `a configured client is resolvable by both id and client id`() {
    val repository =
      TolgeeRegisteredClientRepository(preRegisteredClients(listOf("https://ext.example/callback"), listOf()))

    val client = repository.findByClientId(OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID)
    assertThat(client).isNotNull()
    assertThat(repository.findById(client!!.id)).isEqualTo(client)
  }

  private fun clientsFor(
    extensionUris: List<String>,
    cliUris: List<String>,
  ) = preRegisteredClients(extensionUris, cliUris).clients()

  private fun preRegisteredClients(
    extensionUris: List<String>,
    cliUris: List<String>,
  ) = PreRegisteredClients(
    OAuth2ServerProperties().apply {
      browserExtensionRedirectUris = extensionUris
      cliRedirectUris = cliUris
    },
  )
}
