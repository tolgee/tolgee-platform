package io.tolgee.security.oauth2

import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

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
  fun `every Tolgee scope is allowed and nothing else`() {
    val client = registry(extensionUris = listOf("https://ext.example/callback"), cliUris = listOf()).clients.single()

    Scope.entries.forEach { client.allowsScope(it.value).assert.isTrue() }
    client.allowsScope("not.a.scope").assert.isFalse()
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
