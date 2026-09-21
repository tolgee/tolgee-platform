package io.tolgee.security.oauth2

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.model.enums.Scope
import io.tolgee.security.oauth2.cimd.CimdClient
import io.tolgee.security.oauth2.cimd.CimdClientCache
import io.tolgee.security.oauth2.cimd.CimdClientPolicy
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class OAuth2ClientRegistryTest {
  @Test
  fun `configures no clients when nothing is set and the CLI is turned off`() {
    val registry = registry(extensionUris = listOf(), cliEnabled = false)

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
    val client = registry(extensionUris = listOf("https://ext.example/callback"), cliEnabled = false).clients.single()

    client.allowsRedirectUri("https://ext.example/callback").assert.isTrue()
    client.allowsRedirectUri("https://ext.example/callback/").assert.isFalse()
    client.allowsRedirectUri("https://ext.example/callback?x=1").assert.isFalse()
    client.allowsRedirectUri("https://EXT.example/callback").assert.isFalse()
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
    // Only the port may differ from the registered URI: userInfo and a fragment are both visible to the client.
    client.allowsRedirectUri("http://user@127.0.0.1:9876/callback").assert.isFalse()
    client.allowsRedirectUri("http://127.0.0.1:9876/callback#x").assert.isFalse()
    client.allowsRedirectUri("http://127.0.0.1:9876/callback?next=https://attacker.test").assert.isFalse()
  }

  @Test
  fun `isLoopbackHost matches a localhost redirect on any port, like the IP literals`() {
    val client = registry(extensionUris = listOf(), cliUris = listOf("http://localhost:9876/callback")).clients.single()

    client.allowsRedirectUri("http://localhost:9876/callback").assert.isTrue()
    client.allowsRedirectUri("http://localhost:54321/callback").assert.isTrue()
    client.allowsRedirectUri("http://localhost:9876/other").assert.isFalse()
    client.allowsRedirectUri("http://127.0.0.1:9876/callback").assert.isFalse()
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
    assertThrows<IllegalStateException> { registry(extensionUris = listOf("/callback"), cliUris = listOf()).clients }
  }

  @Test
  fun `plain http is accepted on a loopback host, including localhost`() {
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
    val client = registry(extensionUris = listOf("https://ext.example/callback"), cliEnabled = false).clients.single()

    client.allowsRedirectUri("https://ext.example/call back").assert.isFalse()
  }

  @Test
  fun `a non-loopback redirect is still matched exactly`() {
    val client = registry(extensionUris = listOf("https://ext.example/callback"), cliEnabled = false).clients.single()

    client.allowsRedirectUri("https://ext.example:8443/callback").assert.isFalse()
  }

  @Test
  fun `two redirect spellings share an equivalence key exactly when they accept the same presented URIs`() {
    // The consented-terms hash projects registered redirects through redirectEquivalenceKey, so if the key and the
    // matcher ever disagree the hash either mass-revokes on a neutral edit or misses one that moved the goalposts.
    val spellings =
      listOf(
        "http://127.0.0.1:1234/cb",
        "http://127.0.0.1:9999/cb",
        "http://127.0.0.1:1234/other",
        "http://alice@127.0.0.1:1234/cb",
        "http://127.0.0.1:1234/cb?x=1",
        "http://127.0.0.1:9999/cb?x=1",
        "http://[::1]:1234/cb",
        "https://ext.example/cb",
      )
    val presented = spellings + "http://127.0.0.1:5555/cb" + "http://alice@127.0.0.1:5555/cb"

    for (a in spellings) {
      for (b in spellings) {
        val sameKey =
          OAuth2Client.redirectEquivalenceKey(a) == OAuth2Client.redirectEquivalenceKey(b)
        val accepts = { uri: String -> presented.filter { clientRegistering(uri).allowsRedirectUri(it) } }

        sameKey.assert
          .withFailMessage("%s and %s: same key %s, but same accepted set %s", a, b, sameKey, accepts(a) == accepts(b))
          .isEqualTo(accepts(a) == accepts(b))
      }
    }
  }

  @Test
  fun `a registered loopback entry's own query is not consulted, so it narrows nothing`() {
    val client = clientRegistering("http://127.0.0.1:1234/cb?x=1")

    client.allowsRedirectUri("http://127.0.0.1:1234/cb?x=1").assert.isTrue()
    client.allowsRedirectUri("http://127.0.0.1:9999/cb").assert.isTrue()
    client.allowsRedirectUri("http://127.0.0.1:9999/cb?x=1").assert.isFalse()
  }

  @Test
  fun `an unknown URL-form client id falls through to the CIMD cache`() {
    val cimd = cimdClient(CIMD_URL)
    val cache = mock<CimdClientCache> { on { get(CIMD_URL) } doReturn cimd }
    val registry = registry(extensionUris = listOf("https://ext.example/callback"), cliUris = listOf(), cache = cache)

    registry.find(CIMD_URL).assert.isEqualTo(cimd.client)
    registry.findCimd(CIMD_URL).assert.isEqualTo(cimd)
  }

  @Test
  fun `a pre-registered id never enters the CIMD path`() {
    val cache = mock<CimdClientCache>()
    val registry =
      registry(extensionUris = listOf("https://ext.example/callback"), cliUris = listOf(), cache = cache)

    registry.findCimd(OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID).assert.isNull()
    verify(cache, never()).get(any())
  }

  @Test
  fun `isStillAuthorized accepts a URL-form client without ever fetching it`() {
    val cache = mock<CimdClientCache>()
    val registry = registry(extensionUris = listOf("https://ext.example/callback"), cliUris = listOf(), cache = cache)

    registry.isStillAuthorized(CIMD_URL).assert.isTrue()
    verify(cache, never()).get(any())
  }

  @Test
  fun `a non-empty allowed-hosts list restricts which hosts may present a document`() {
    val cache = mock<CimdClientCache> { on { get(any()) } doReturn cimdClient(CIMD_URL) }
    val registry =
      registry(
        extensionUris = listOf("https://ext.example/callback"),
        cimdAllowedHosts = listOf("app.example.com"),
        cache = cache,
      )

    registry.find(CIMD_URL).assert.isNotNull
    registry.find("https://evil.example/.well-known/client").assert.isNull()
    registry.isStillAuthorized("https://evil.example/.well-known/client").assert.isFalse()
    verify(cache, never()).get("https://evil.example/.well-known/client")
  }

  @Test
  fun `enabling is issuer-based, so no pre-registered client is required`() {
    val registry = registry()

    // Nothing is configured here: the CLI is seeded on every instance with an issuer, and CIMD is live regardless.
    registry.clients
      .map { it.clientId }
      .assert
      .containsExactly(OAuth2Constants.CLI_CLIENT_ID)
    registry.isStillAuthorized(CIMD_URL).assert.isTrue()
  }

  @Test
  fun `an instance with no usable issuer resolves no CIMD client and authorizes no CIMD grant`() {
    val registry = registry(resolver = resolver(isConfigured = false))

    registry.find(CIMD_URL).assert.isNull()
    registry.isStillAuthorized(CIMD_URL).assert.isFalse()
  }

  @Test
  fun `the CLI is registered without anyone configuring it`() {
    val registry = registry()

    val cli = registry.find(OAuth2Constants.CLI_CLIENT_ID)

    cli.assert.isNotNull()
    // The CLI takes whatever port the OS gives it, so the registered one cannot be part of the comparison.
    cli!!.allowsRedirectUri("http://127.0.0.1:53211/callback").assert.isTrue()
  }

  @Test
  fun `an instance that will never see the CLI can turn it off`() {
    val registry = registry(cliEnabled = false)

    registry.find(OAuth2Constants.CLI_CLIENT_ID).assert.isNull()
  }

  @Test
  fun `turning the CLI off also refuses redirect URIs configured for it`() {
    val registry = registry(cliUris = listOf("https://cli.example/callback"), cliEnabled = false)

    registry.find(OAuth2Constants.CLI_CLIENT_ID).assert.isNull()
  }

  @Test
  fun `configured redirect URIs replace the default rather than adding to it`() {
    val registry = registry(cliUris = listOf("https://cli.example/callback"))

    val cli = registry.find(OAuth2Constants.CLI_CLIENT_ID)!!

    cli.allowsRedirectUri("https://cli.example/callback").assert.isTrue()
    cli.allowsRedirectUri("http://127.0.0.1:53211/callback").assert.isFalse()
  }

  @Test
  fun `the CLI is left out where the issuer does not resolve`() {
    val registry = registry(resolver = resolver(isConfigured = false))

    registry.find(OAuth2Constants.CLI_CLIENT_ID).assert.isNull()
  }

  @Test
  fun `a pre-registered client still requires a usable issuer at startup`() {
    val registry = registry(extensionUris = listOf("https://ext.example/callback"), resolver = throwingResolver())

    val failure = assertThrows<IllegalStateException> { registry.requireIssuerForPreRegisteredClients() }
    failure.message.assert.contains(UNUSABLE_ISSUER)
  }

  @Test
  fun `an instance that configured no client boots even when the issuer is unusable`() {
    val registry = registry(resolver = throwingResolver())

    assertThatCode { registry.requireIssuerForPreRegisteredClients() }.doesNotThrowAnyException()
  }

  private fun throwingResolver(): OAuth2IssuerResolver =
    mock {
      on { issuerUrl } doThrow IllegalStateException("must be a bare origin, got: $UNUSABLE_ISSUER")
    }

  private fun clientRegistering(redirectUri: String) =
    OAuth2Client(clientId = "c", name = "c", redirectUris = listOf(redirectUri))

  private fun cimdClient(url: String) =
    CimdClient(
      OAuth2Client(clientId = url, name = url, redirectUris = listOf("$url/cb"), verified = false, metadataHash = "h"),
      clientOrigin = url,
    )

  private fun registry(
    extensionUris: List<String> = listOf(),
    cliUris: List<String> = listOf(),
    cliEnabled: Boolean = true,
    cimdAllowedHosts: List<String> = listOf(),
    cache: CimdClientCache = mock(),
    resolver: OAuth2IssuerResolver = resolver(),
  ): OAuth2ClientRegistry {
    val properties =
      OAuth2ServerProperties().apply {
        browserExtensionRedirectUris = extensionUris
        cliRedirectUris = cliUris
        this.cliEnabled = cliEnabled
        this.cimdAllowedHosts = cimdAllowedHosts
      }
    return OAuth2ClientRegistry(properties, cache, CimdClientPolicy(properties, InternalProperties()), resolver)
  }

  private fun resolver(isConfigured: Boolean = true): OAuth2IssuerResolver =
    mock {
      on { this.isConfigured } doReturn isConfigured
      on { issuerUrl } doReturn "https://tolgee.example.com"
    }

  companion object {
    private const val CIMD_URL = "https://app.example.com/.well-known/oauth-client"
    private const val UNUSABLE_ISSUER = "https://tools.acme.com/tolgee"
  }
}
