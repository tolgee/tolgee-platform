package io.tolgee.security.oauth2

import com.sun.net.httpserver.HttpServer
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.oauth2.cimd.CimdClientCache
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * The whole CIMD journey through the real HTTP stack: an unknown client presents an HTTPS (here, dev-loopback) URL as
 * its `client_id`, the authorization server fetches and validates the document at that URL, and a token is issued
 * against the unverified client it describes. SSRF protection is disabled so the metadata can be served on loopback,
 * which is exactly the dev escape hatch the fetcher documents.
 */
@TestPropertySource(properties = ["tolgee.internal.disable-url-ssrf-protection=true"])
class OAuth2CimdFlowTest : AbstractOAuth2FlowTest() {
  @Autowired
  private lateinit var resources: OAuth2Resources

  @Autowired
  private lateinit var cimdClientCache: CimdClientCache

  @Autowired
  private lateinit var grantRepository: OAuth2GrantRepository

  private var server: HttpServer? = null

  /** Set mid-test to make the served document declare a redirect the user never consented to. */
  private var extraRedirectUri: String? = null

  @AfterEach
  fun stopServer() {
    server?.stop(0)
    server = null
    extraRedirectUri = null
  }

  @Test
  fun `an unknown client resolves through its metadata document and is issued an MCP-audience token`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val mcpResource = resources.mcpResource

    driver
      .authorize(clientId, redirect, validParams(mcpResource))
      .andReturn()
      .response.status.assert
      .isEqualTo(302)

    val pending =
      driver.startPendingConsent(jwt(), clientId, redirect, scope = "translations.view", resource = mcpResource)
    val code = driver.code(pending, projectId = null)
    val token =
      json(driver.exchangeCode(code, clientId, redirect, pending.verifier, resource = mcpResource))
        .get("access_token")
        .asString()

    token.assert.isNotBlank()
    val grant = stored(token)
    grant.clientMetadataHash.assert.isNotNull()
    grant.boundAudience().assert.isEqualTo(OAuth2Audience.MCP)
  }

  @Test
  fun `the consent screen is told the client is unverified, with the origin and same-origin logo the document gave`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p, logoUri = "http://127.0.0.1:$p/l") }
    val clientId = clientIdAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))

    val info = consentInfo(jwt(), pending.state)

    info
      .get("verified")
      .asBoolean()
      .assert
      .isFalse()
    info
      .get("appName")
      .asString()
      .assert
      .isEqualTo("Loopback Test Client")
    info
      .get("clientOrigin")
      .asString()
      .assert
      .isEqualTo("http://127.0.0.1:$port")
    info
      .get("logoUri")
      .asString()
      .assert
      .isEqualTo("http://127.0.0.1:$port/l")
  }

  @Test
  fun `a logo hosted somewhere other than the client's own origin is not handed to the consent screen`() {
    val port =
      serveDocument { p -> validDocument(clientIdAt(p), p, logoUri = "https://cdn.other.example/l.png") }
    val clientId = clientIdAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))

    consentInfo(jwt(), pending.state)
      .get("logoUri")
      .isNull.assert
      .isTrue()
  }

  @Test
  fun `a grant survives a refresh once the client's metadata host stops answering`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val code = driver.code(pending, projectId = null)
    val issued = json(driver.exchangeCode(code, clientId, redirect, pending.verifier))
    val grantId = stored(issued.get("access_token").asString()).id

    stopServer()
    // Without this the 300s cache answers from the resolution the authorize hop already made, and the refresh never
    // reaches the path this test exists for.
    cimdClientCache.invalidate(clientId)

    json(driver.refresh(issued.get("refresh_token").asString(), clientId))
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
    grantRepository.existsById(grantId).assert.isTrue()
  }

  @Test
  fun `widening the redirect set after consent revokes the grant at its next refresh`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val code = driver.code(pending)
    val issued = json(driver.exchangeCode(code, clientId, redirect, pending.verifier))
    val grantId = stored(issued.get("access_token").asString()).id

    extraRedirectUri = "http://127.0.0.1:$port/attacker-cb"
    cimdClientCache.invalidate(clientId)

    json(driver.refresh(issued.get("refresh_token").asString(), clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    grantRepository.existsById(grantId).assert.isFalse()
  }

  @Test
  fun `the consent screen still renders once the client's metadata host stops answering`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))

    stopServer()
    cimdClientCache.invalidate(clientId)

    val info = consentInfo(jwt(), pending.state)

    info
      .get("verified")
      .asBoolean()
      .assert
      .isFalse()
    info
      .get("appName")
      .asString()
      .assert
      .isEqualTo(clientId)
    info
      .get("clientOrigin")
      .isNull.assert
      .isTrue()
    info
      .get("logoUri")
      .isNull.assert
      .isTrue()
  }

  @Test
  fun `a token request for a CIMD client whose document does not resolve is invalid_grant, not invalid_client`() {
    json(driver.refresh("tgort_no-such-grant", "https://unresolvable.invalid/client"))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a document whose client_id does not match the fetched URL is refused`() {
    val port = serveDocument { p -> validDocument("https://not-this-url.example/client", p) }
    val clientId = clientIdAt(port)

    driver
      .authorize(clientId, redirectAt(port), validParams(null))
      .andReturn()
      .response.status.assert
      .isEqualTo(400)
  }

  private fun clientIdAt(port: Int) = "http://127.0.0.1:$port/client"

  private fun redirectAt(port: Int) = "http://127.0.0.1:$port/cb"

  private fun validParams(resource: String?): Map<String, String?> =
    mapOf(
      "response_type" to "code",
      "scope" to "translations.view",
      "code_challenge" to OAuth2FlowDriver.randomChallenge(),
      "code_challenge_method" to "S256",
      "resource" to resource,
    )

  private fun validDocument(
    clientIdField: String,
    port: Int,
    logoUri: String? = null,
  ): String {
    val logo = logoUri?.let { ""","logo_uri": "$it"""" }.orEmpty()
    val extra = extraRedirectUri?.let { ""","$it"""" }.orEmpty()
    return """
      {
        "client_id": "$clientIdField",
        "client_name": "Loopback Test Client",
        "token_endpoint_auth_method": "none",
        "grant_types": ["authorization_code", "refresh_token"],
        "redirect_uris": ["${redirectAt(port)}"$extra]$logo
      }
      """.trimIndent()
  }

  /** Serves the document at `/client`; the builder gets the bound port so redirect and logo can point back at it. */
  private fun serveDocument(document: (port: Int) -> String): Int {
    val created = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
    val port = created.address.port
    created.createContext("/client") { exchange ->
      val body = document(port).toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.use { it.write(body) }
    }
    created.start()
    server = created
    return port
  }
}
