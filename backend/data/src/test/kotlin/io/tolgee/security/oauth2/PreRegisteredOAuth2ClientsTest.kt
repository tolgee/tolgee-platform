package io.tolgee.security.oauth2

import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock

class PreRegisteredOAuth2ClientsTest {
  @Test
  fun `configures no clients when nothing is set and the CLI is turned off`() {
    val clients = clients(extensionUris = listOf(), cliEnabled = false)

    clients.all.assert.isEmpty()
    clients.find(OAuth2Constants.CLI_CLIENT_ID).assert.isNull()
  }

  @Test
  fun `configures the extension and CLI clients when redirect URIs are set`() {
    val clients =
      clients(
        extensionUris = listOf("https://ext.example/callback"),
        cliUris = listOf("http://127.0.0.1:9876/callback"),
      )

    val extension = clients.find(OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID)
    extension.assert.isNotNull
    extension!!.redirectUris.assert.containsExactly("https://ext.example/callback")
    extension.requiredScopes.assert.containsExactlyInAnyOrder(Scope.KEYS_VIEW, Scope.TRANSLATIONS_VIEW)

    val cli = clients.find(OAuth2Constants.CLI_CLIENT_ID)
    cli.assert.isNotNull
    cli!!.redirectUris.assert.containsExactly("http://127.0.0.1:9876/callback")
    cli.requiredScopes.assert.isEmpty()
  }

  @Test
  fun `a redirect URI must match a registered one exactly`() {
    val client = clients(extensionUris = listOf("https://ext.example/callback"), cliEnabled = false).all.single()

    client.allowsRedirectUri("https://ext.example/callback").assert.isTrue()
    client.allowsRedirectUri("https://ext.example/callback/").assert.isFalse()
    client.allowsRedirectUri("https://ext.example/callback?x=1").assert.isFalse()
    client.allowsRedirectUri("https://EXT.example/callback").assert.isFalse()
  }

  @Test
  fun `a loopback redirect is accepted on any port (RFC 8252 section 7-3)`() {
    val client = clients(extensionUris = listOf(), cliUris = listOf("http://127.0.0.1:9876/callback")).all.single()

    client.allowsRedirectUri("http://127.0.0.1:9876/callback").assert.isTrue()
    client.allowsRedirectUri("http://127.0.0.1:54321/callback").assert.isTrue()
    client.allowsRedirectUri("http://127.0.0.1/callback").assert.isTrue()

    client.allowsRedirectUri("http://127.0.0.1:9876/other").assert.isFalse()
    client.allowsRedirectUri("https://127.0.0.1:9876/callback").assert.isFalse()
    client.allowsRedirectUri("http://attacker.test:9876/callback").assert.isFalse()
    // Only the port may differ from the registered URI: userInfo and a fragment are both visible to the client.
    client.allowsRedirectUri("http://user@127.0.0.1:9876/callback").assert.isFalse()
    client.allowsRedirectUri("http://127.0.0.1:9876/callback#x").assert.isFalse()
    client.allowsRedirectUri("http://127.0.0.1:9876/callback?next=https://attacker.test").assert.isFalse()
  }

  @Test
  fun `isLoopbackHost matches a localhost redirect on any port, like the IP literals`() {
    val client = clients(extensionUris = listOf(), cliUris = listOf("http://localhost:9876/callback")).all.single()

    client.allowsRedirectUri("http://localhost:9876/callback").assert.isTrue()
    client.allowsRedirectUri("http://localhost:54321/callback").assert.isTrue()
    client.allowsRedirectUri("http://localhost:9876/other").assert.isFalse()
    client.allowsRedirectUri("http://127.0.0.1:9876/callback").assert.isFalse()
  }

  @Test
  fun `an IPv6 loopback redirect is accepted on any port`() {
    val client = clients(extensionUris = listOf(), cliUris = listOf("http://[::1]:9876/callback")).all.single()

    client.allowsRedirectUri("http://[::1]:9876/callback").assert.isTrue()
    client.allowsRedirectUri("http://[::1]:54321/callback").assert.isTrue()
    client.allowsRedirectUri("http://[::1]:9876/other").assert.isFalse()
    client.allowsRedirectUri("http://[::2]:9876/callback").assert.isFalse()
  }

  @Test
  fun `a configured redirect URI that is not absolute is refused at startup`() {
    assertThrows<IllegalStateException> { clients(extensionUris = listOf("/callback"), cliUris = listOf()).all }
  }

  @Test
  fun `plain http is accepted on a loopback host, including localhost`() {
    clients(extensionUris = listOf("http://localhost:8201/callback"), cliUris = listOf()).all.assert.isNotEmpty
    clients(extensionUris = listOf(), cliUris = listOf("http://127.0.0.1:9876/cb")).all.assert.isNotEmpty
    clients(extensionUris = listOf(), cliUris = listOf("http://[::1]:9876/cb")).all.assert.isNotEmpty
  }

  @Test
  fun `a configured redirect URI carrying a fragment or plain http is refused at startup`() {
    assertThrows<IllegalStateException> {
      clients(extensionUris = listOf("https://ext.example/cb#x"), cliUris = listOf()).all
    }
    assertThrows<IllegalStateException> {
      clients(extensionUris = listOf("http://ext.example/cb"), cliUris = listOf()).all
    }
  }

  @Test
  fun `a presented redirect URI that does not parse never matches`() {
    val client = clients(extensionUris = listOf("https://ext.example/callback"), cliEnabled = false).all.single()

    client.allowsRedirectUri("https://ext.example/call back").assert.isFalse()
  }

  @Test
  fun `a non-loopback redirect is still matched exactly`() {
    val client = clients(extensionUris = listOf("https://ext.example/callback"), cliEnabled = false).all.single()

    client.allowsRedirectUri("https://ext.example:8443/callback").assert.isFalse()
  }

  @Test
  fun `enabling is issuer-based, so no pre-registered client is required`() {
    val clients = clients()

    clients.all
      .map { it.clientId }
      .assert
      .containsExactly(OAuth2Constants.CLI_CLIENT_ID)
  }

  @Test
  fun `the CLI is registered without anyone configuring it`() {
    val clients = clients()

    val cli = clients.find(OAuth2Constants.CLI_CLIENT_ID)

    cli.assert.isNotNull()
    cli!!.allowsRedirectUri("http://127.0.0.1:53211/callback").assert.isTrue()
  }

  @Test
  fun `an instance that will never see the CLI can turn it off`() {
    val clients = clients(cliEnabled = false)

    clients.find(OAuth2Constants.CLI_CLIENT_ID).assert.isNull()
  }

  @Test
  fun `turning the CLI off also refuses redirect URIs configured for it`() {
    val clients = clients(cliUris = listOf("https://cli.example/callback"), cliEnabled = false)

    clients.find(OAuth2Constants.CLI_CLIENT_ID).assert.isNull()
  }

  @Test
  fun `configured redirect URIs replace the default rather than adding to it`() {
    val clients = clients(cliUris = listOf("https://cli.example/callback"))

    val cli = clients.find(OAuth2Constants.CLI_CLIENT_ID)!!

    cli.allowsRedirectUri("https://cli.example/callback").assert.isTrue()
    cli.allowsRedirectUri("http://127.0.0.1:53211/callback").assert.isFalse()
  }

  @Test
  fun `the CLI is left out where the issuer does not resolve`() {
    val clients = clients(resolver = resolver(isConfigured = false))

    clients.find(OAuth2Constants.CLI_CLIENT_ID).assert.isNull()
  }

  @Test
  fun `the extension is off until an operator turns it on`() {
    clients().find(OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID).assert.isNull()
  }

  @Test
  fun `turning the extension on registers the published extension's redirect URIs`() {
    val extension = clients(extensionEnabled = true).find(OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID)

    extension.assert.isNotNull()
    extension!!.redirectUris.assert.containsExactlyElementsOf(OAuth2Constants.OFFICIAL_BROWSER_EXTENSION_REDIRECT_URIS)
  }

  @Test
  fun `configured extension redirect URIs replace the published ones rather than adding to them`() {
    val extension =
      clients(extensionEnabled = true, extensionUris = listOf("https://ext.example/callback"))
        .find(OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID)!!

    extension.allowsRedirectUri("https://ext.example/callback").assert.isTrue()
    OAuth2Constants.OFFICIAL_BROWSER_EXTENSION_REDIRECT_URIS.forEach {
      extension
        .allowsRedirectUri(
          it,
        ).assert
        .isFalse()
    }
  }

  @Test
  fun `turning the extension off also refuses redirect URIs configured for it`() {
    val clients = clients(extensionEnabled = false, extensionUris = listOf("https://ext.example/callback"))

    clients.find(OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID).assert.isNull()
  }

  @Test
  fun `turning the extension on requires a usable issuer at startup`() {
    val clients = clients(extensionEnabled = true, resolver = throwingResolver())

    assertThrows<IllegalStateException> { clients.requireIssuerForConfiguredClients() }
  }

  @Test
  fun `a pre-registered client still requires a usable issuer at startup`() {
    val clients = clients(extensionUris = listOf("https://ext.example/callback"), resolver = throwingResolver())

    val failure = assertThrows<IllegalStateException> { clients.requireIssuerForConfiguredClients() }
    failure.message.assert.contains(UNUSABLE_ISSUER)
  }

  @Test
  fun `an instance that configured no client boots even when the issuer is unusable`() {
    val clients = clients(resolver = throwingResolver())

    assertThatCode { clients.requireIssuerForConfiguredClients() }.doesNotThrowAnyException()
  }

  private fun throwingResolver(): OAuth2IssuerResolver =
    mock {
      on { issuerUrl } doThrow IllegalStateException("must be a bare origin, got: $UNUSABLE_ISSUER")
    }

  /** Configured extension URIs imply the operator turned the extension on, as the test configs do. */
  private fun clients(
    extensionUris: List<String> = listOf(),
    extensionEnabled: Boolean = extensionUris.isNotEmpty(),
    cliUris: List<String> = listOf(),
    cliEnabled: Boolean = true,
    resolver: OAuth2IssuerResolver = resolver(),
  ): PreRegisteredOAuth2Clients {
    val properties =
      OAuth2ServerProperties().apply {
        browserExtensionEnabled = extensionEnabled
        browserExtensionRedirectUris = extensionUris
        cliRedirectUris = cliUris
        this.cliEnabled = cliEnabled
      }
    return PreRegisteredOAuth2Clients(properties, resolver)
  }

  private fun resolver(isConfigured: Boolean = true): OAuth2IssuerResolver =
    mock {
      on { this.isConfigured } doReturn isConfigured
      on { issuerUrl } doReturn "https://tolgee.example.com"
    }

  companion object {
    private const val UNUSABLE_ISSUER = "https://tools.acme.com/tolgee"
  }
}
