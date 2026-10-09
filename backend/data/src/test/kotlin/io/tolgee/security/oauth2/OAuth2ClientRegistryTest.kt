package io.tolgee.security.oauth2

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.security.oauth2.cimd.CimdClient
import io.tolgee.security.oauth2.cimd.CimdClientCache
import io.tolgee.security.oauth2.cimd.CimdClientPolicy
import io.tolgee.security.oauth2.cimd.CimdResolution
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class OAuth2ClientRegistryTest {
  @Test
  fun `a redirect to the user's own machine is reported as one`() {
    OAuth2Client.redirectsToLocalApp("http://127.0.0.1:53211/callback").assert.isTrue()
    OAuth2Client.redirectsToLocalApp("http://localhost:53211/callback").assert.isTrue()
    OAuth2Client.redirectsToLocalApp("http://[::1]:53211/callback").assert.isTrue()
  }

  @Test
  fun `a redirect to a website is not`() {
    OAuth2Client.redirectsToLocalApp("https://app.example/callback").assert.isFalse()
    OAuth2Client.redirectsToLocalApp("https://127.0.0.1.evil.example/callback").assert.isFalse()
    OAuth2Client.redirectsToLocalApp("not a uri").assert.isFalse()
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
    val cache = mock<CimdClientCache> { on { get(eq(CIMD_URL)) } doReturn cimd }
    val registry = registry(extensionUris = listOf("https://ext.example/callback"), cliUris = listOf(), cache = cache)

    registry.find(CIMD_URL).assert.isEqualTo(cimd.client)
    registry.findCimd(CIMD_URL).assert.isEqualTo(cimd)
  }

  @Test
  fun `resolving a client on the authorize path asks nothing about who holds grants`() {
    val cache = mock<CimdClientCache> { on { get(any()) } doReturn null }

    registry(cache = cache).find(CIMD_URL)

    verify(cache).get(CIMD_URL)
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
  fun `a client whose document is not in the cache keeps its grant, so a blip cannot kill one`() {
    val cache = mock<CimdClientCache> { on { cachedClient(any()) } doReturn null }
    val registry = registry(extensionUris = listOf("https://ext.example/callback"), cliUris = listOf(), cache = cache)

    registry.findForExistingGrant(CIMD_URL).assert.isNotNull
    registry.servesClient(CIMD_URL).assert.isTrue()
  }

  @Test
  fun `only a client this instance still serves passes`() {
    val registry = registry(cimdAllowedHosts = listOf("allowed.example"))

    registry.servesClient("not-a-url-and-not-registered").assert.isFalse()
    registry.servesClient(CIMD_URL).assert.isFalse()
    registry.servesClient(OAuth2Constants.CLI_CLIENT_ID).assert.isTrue()
  }

  /**
   * It runs in AuthenticationFilter, on every request carrying an OAuth token and ahead of every rate limiter.
   * Reading a document here would put a DNS lookup and an HTTPS GET to a host the token holder chose on that path.
   */

  @Test
  fun `the per-request check reads no cache and never fetches`() {
    val cache = mock<CimdClientCache>()
    val registry = registry(cache = cache)

    registry.servesClient(CIMD_URL).assert.isTrue()

    verify(cache, never()).cachedClient(any())
    verify(cache, never()).fetchOnGrantLane(any())
    verify(cache, never()).get(any())
  }

  @Test
  fun `the token path never reads a document`() {
    val cache = mock<CimdClientCache> { on { cachedClient(any()) } doReturn null }

    registry(cache = cache)
      .findForExistingGrant(CIMD_URL)!!
      .verified.assert
      .isFalse()

    verify(cache, never()).fetchOnGrantLane(any())
    verify(cache, never()).get(any())
  }

  @Test
  fun `the document check reads the document and leaves the request lane untouched`() {
    val cache = mock<CimdClientCache> { on { fetchOnGrantLane(any()) } doReturn CimdResolution.Withdrawn }
    val registry = registry(cache = cache)

    registry.resolveForCheck(CIMD_URL).assert.isEqualTo(CimdResolution.Withdrawn)

    verify(cache).fetchOnGrantLane(CIMD_URL)
    // Evicting would make the next `/oauth2/authorize` poll for this client slow, which says a grant for it exists.
    verify(cache, never()).invalidate(any())
    verify(cache, never()).get(any())
  }

  @Test
  fun `a client_id the CIMD path does not serve is not one the document check may read`() {
    val registry = registry(cimdAllowedHosts = listOf("allowed.example"))

    registry.servesCimdClient(CIMD_URL).assert.isFalse()
  }

  @Test
  fun `a resolved client in the cache is what an existing grant's lookup answers with`() {
    val cache = mock<CimdClientCache> { on { cachedClient(any()) } doReturn cimdClient(CIMD_URL) }
    val registry = registry(cache = cache)

    val client = registry.findForExistingGrant(CIMD_URL)

    client!!
      .clientId.assert
      .isEqualTo(CIMD_URL)
    client.metadataHash.assert.isEqualTo("h")
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
    verify(cache, never()).get("https://evil.example/.well-known/client")
  }

  @Test
  fun `an instance with no usable issuer resolves no CIMD client and authorizes no CIMD grant`() {
    val registry = registry(resolver = resolver(isConfigured = false))

    registry.find(CIMD_URL).assert.isNull()
  }

  private fun clientRegistering(redirectUri: String) =
    OAuth2Client(clientId = "c", name = "c", redirectUris = listOf(redirectUri))

  private fun cimdClient(url: String) =
    CimdClient(
      OAuth2Client(clientId = url, name = url, redirectUris = listOf("$url/cb"), verified = false, metadataHash = "h"),
      clientOrigin = url,
    )

  /** Configured extension URIs imply the operator turned the extension on, as the test configs do. */
  private fun registry(
    extensionUris: List<String> = listOf(),
    extensionEnabled: Boolean = extensionUris.isNotEmpty(),
    cliUris: List<String> = listOf(),
    cliEnabled: Boolean = true,
    cimdAllowedHosts: List<String> = listOf(),
    cache: CimdClientCache = mock { on { fetchOnGrantLane(any()) } doReturn CimdResolution.Unavailable },
    resolver: OAuth2IssuerResolver = resolver(),
  ): OAuth2ClientRegistry {
    val properties =
      OAuth2ServerProperties().apply {
        browserExtensionEnabled = extensionEnabled
        browserExtensionRedirectUris = extensionUris
        cliRedirectUris = cliUris
        this.cliEnabled = cliEnabled
        this.cimdAllowedHosts = cimdAllowedHosts
      }
    return OAuth2ClientRegistry(
      PreRegisteredOAuth2Clients(properties, resolver),
      cache,
      CimdClientPolicy(properties, InternalProperties(), resolver, mock { on { stableUrl } doReturn null }),
      resolver,
    )
  }

  private fun resolver(isConfigured: Boolean = true): OAuth2IssuerResolver =
    mock {
      on { this.isConfigured } doReturn isConfigured
      on { issuerUrl } doReturn "https://tolgee.example.com"
    }

  companion object {
    private const val CIMD_URL = "https://app.example.com/.well-known/oauth-client"
  }
}
