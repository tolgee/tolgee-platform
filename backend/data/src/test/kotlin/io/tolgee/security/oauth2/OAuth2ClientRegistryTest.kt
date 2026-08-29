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
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OAuth2ClientRegistryTest {
  @Test
  fun `configures no clients when no redirect URIs are set`() {
    // Emptying the config is how an operator disables a client, and nothing is persisted, so it simply stops existing.
    val registry = registry(extensionUris = listOf(), cliUris = listOf())

    registry.clients.assert.isEmpty()
    registry.find(OAuth2Constants.CLI_CLIENT_ID).assert.isNull()
  }

  @Test
  fun `configures the extension and CLI clients when redirect URIs are set`() {
    val registry =
      registry(
        extensionUris = listOf("https://ext.example/callback"),
        cliUris = listOf("http://127.0.0.1:9876/callback"),
      )

    val extension = registry.find(OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID)
    extension.assert.isNotNull
    extension!!.redirectUris.assert.containsExactly("https://ext.example/callback")
    extension.requiredScopes.assert.containsExactlyInAnyOrder(Scope.KEYS_VIEW, Scope.TRANSLATIONS_VIEW)

    val cli = registry.find(OAuth2Constants.CLI_CLIENT_ID)
    cli.assert.isNotNull
    cli!!.redirectUris.assert.containsExactly("http://127.0.0.1:9876/callback")
    cli.requiredScopes.assert.isEmpty()
  }

  @Test
  fun `a redirect URI must match a registered one exactly`() {
    val client = registry(extensionUris = listOf("https://ext.example/callback"), cliUris = listOf()).clients.single()

    client.allowsRedirectUri("https://ext.example/callback").assert.isTrue()
    client.allowsRedirectUri("https://ext.example/callback/").assert.isFalse()
    client.allowsRedirectUri("https://ext.example/callback?x=1").assert.isFalse()
    client.allowsRedirectUri("https://EXT.example/callback").assert.isFalse()
  }

  @Test
  fun `a scope is supported only when it is one of Tolgee's own`() {
    OAuth2Scopes.isSupported("translations.view").assert.isTrue()
    OAuth2Scopes.isSupported("translations.edit").assert.isTrue()
    OAuth2Scopes.isSupported("not.a.scope").assert.isFalse()
    OAuth2Scopes.isSupported("TRANSLATIONS_VIEW").assert.isFalse()
  }

  @Test
  fun `a loopback redirect is accepted on any port (RFC 8252 section 7-3)`() {
    val client = registry(extensionUris = listOf(), cliUris = listOf("http://127.0.0.1:9876/callback")).clients.single()

    client.allowsRedirectUri("http://127.0.0.1:9876/callback").assert.isTrue()
    client.allowsRedirectUri("http://127.0.0.1:54321/callback").assert.isTrue()
    client.allowsRedirectUri("http://127.0.0.1/callback").assert.isTrue()

    client.allowsRedirectUri("http://127.0.0.1:9876/other").assert.isFalse()
    client.allowsRedirectUri("https://127.0.0.1:9876/callback").assert.isFalse()
    client.allowsRedirectUri("http://attacker.test:9876/callback").assert.isFalse()
    client.allowsRedirectUri("http://127.0.0.1:9876/callback?next=https://attacker.test").assert.isFalse()
  }

  @Test
  fun `an IPv6 loopback redirect is accepted on any port`() {
    val client = registry(extensionUris = listOf(), cliUris = listOf("http://[::1]:9876/callback")).clients.single()

    client.allowsRedirectUri("http://[::1]:9876/callback").assert.isTrue()
    client.allowsRedirectUri("http://[::1]:54321/callback").assert.isTrue()
    client.allowsRedirectUri("http://[::1]:9876/other").assert.isFalse()
    client.allowsRedirectUri("http://[::2]:9876/callback").assert.isFalse()
  }

  @Test
  fun `a configured redirect URI that is not absolute is refused at startup`() {
    // A relative entry parses and would match, and the code redirect would then resolve against Tolgee's own origin.
    assertThrows<IllegalStateException> { registry(extensionUris = listOf("/callback"), cliUris = listOf()).clients }
  }

  @Test
  fun `plain http is accepted on a loopback host, including localhost`() {
    // e2e and every local client are configured with http://localhost; refusing it would reject a legitimate
    // deployment rather than an unsafe one.
    registry(extensionUris = listOf("http://localhost:8201/callback"), cliUris = listOf()).clients.assert.isNotEmpty
    registry(extensionUris = listOf(), cliUris = listOf("http://127.0.0.1:9876/cb")).clients.assert.isNotEmpty
    registry(extensionUris = listOf(), cliUris = listOf("http://[::1]:9876/cb")).clients.assert.isNotEmpty
  }

  @Test
  fun `a configured redirect URI carrying a fragment or plain http is refused at startup`() {
    assertThrows<IllegalStateException> {
      registry(extensionUris = listOf("https://ext.example/cb#x"), cliUris = listOf()).clients
    }
    assertThrows<IllegalStateException> {
      registry(extensionUris = listOf("http://ext.example/cb"), cliUris = listOf()).clients
    }
  }

  @Test
  fun `a presented redirect URI that does not parse never matches`() {
    val client = registry(extensionUris = listOf("https://ext.example/callback"), cliUris = listOf()).clients.single()

    client.allowsRedirectUri("https://ext.example/call back").assert.isFalse()
  }

  @Test
  fun `a non-loopback redirect is still matched exactly`() {
    val client = registry(extensionUris = listOf("https://ext.example/callback"), cliUris = listOf()).clients.single()

    client.allowsRedirectUri("https://ext.example:8443/callback").assert.isFalse()
  }

  private fun registry(
    extensionUris: List<String>,
    cliUris: List<String>,
  ) = OAuth2ClientRegistry(
    OAuth2ServerProperties().apply {
      browserExtensionRedirectUris = extensionUris
      cliRedirectUris = cliUris
    },
  )
}
