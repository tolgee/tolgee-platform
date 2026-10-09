package io.tolgee.security.oauth2

import com.sun.net.httpserver.HttpServer
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.bearerHeaders
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.oauth2.cimd.CimdClientCache
import io.tolgee.security.oauth2.cimd.CimdClientLifecycleService
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * The whole CIMD journey through the real HTTP stack: an unknown client presents an HTTPS (here, dev-loopback) URL as
 * its `client_id`, the authorization server fetches and validates the document at that URL, and a token is issued
 * against the unverified client it describes. SSRF protection is disabled so the metadata can be served on loopback,
 * which is exactly the dev escape hatch the fetcher documents.
 */
@TestPropertySource(properties = ["tolgee.internal.disable-url-ssrf-protection=true"])
class OAuth2CimdFlowTest : AbstractOAuth2FlowTest() {
  @Autowired
  private lateinit var oauth2Resources: OAuth2Resources

  @Autowired
  private lateinit var cimdClientCache: CimdClientCache

  @Autowired
  private lateinit var oauth2GrantRepository: OAuth2GrantRepository

  @Autowired
  private lateinit var cimdClientLifecycle: CimdClientLifecycleService

  @Autowired
  private lateinit var oauth2Properties: OAuth2ServerProperties

  private var server: HttpServer? = null

  /** Set mid-test to make the served document declare a redirect the user never consented to. */
  private var extraRedirectUri: String? = null

  /** A publisher retiring the client: the document is taken down, so the host answers but the document does not. */
  private var documentWithdrawn: Boolean = false

  /** The publisher's origin having a bad minute: it answers, but not with the document. */
  private var documentStatus: Int? = null

  /** A second client on the same host, so one account can hold two document-backed clients. */
  private var otherDocumentBroken: Boolean = false

  /** How often the publisher was asked for the main client's document. */
  private val documentRequests = AtomicInteger()

  @AfterEach
  fun resetFixture() {
    stopServer()
    extraRedirectUri = null
    documentWithdrawn = false
    documentStatus = null
    otherDocumentBroken = false
    documentRequests.set(0)
    oauth2Properties.cimdMaxClientsPerUser = OAuth2ServerProperties().cimdMaxClientsPerUser
    currentDateProvider.forcedDate = null
  }

  private fun stopServer() {
    server?.stop(0)
    server = null
  }

  @Test
  fun `an unknown client resolves through its metadata document and is issued an MCP-audience token`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val mcpResource = oauth2Resources.mcpResource

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
  fun `the consent screen is told the client is unverified, with the origin the document came from`() {
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
  }

  @Test
  fun `the consent screen is never given a logo to load from the app's own server`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p, logoUri = "http://127.0.0.1:$p/l") }
    val clientId = clientIdAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))

    consentInfo(jwt(), pending.state).has("logoUri").assert.isFalse()
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

    json(driver.refresh(issued.get("refresh_token").asString(), clientId))
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
    oauth2GrantRepository.existsById(grantId).assert.isTrue()
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

    json(driver.refresh(issued.get("refresh_token").asString(), clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    oauth2GrantRepository.existsById(grantId).assert.isFalse()
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
  }

  @Test
  fun `a withdrawal seen on a refresh is written to the grant, so the access token dies with it`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))
    val code = driver.code(pending, projectId = testData.project.id)
    val tokens = json(driver.exchangeCode(code, clientId, redirectAt(port), pending.verifier))
    val accessToken = tokens.get("access_token").asString()
    performGet("/v2/projects/${testData.project.id}/translations", bearerHeaders(accessToken)).andIsOk

    documentWithdrawn = true

    json(driver.refresh(tokens.get("refresh_token").asString(), clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")

    stored(accessToken).clientWithdrawnAt.assert.isNotNull()
    performGet("/v2/projects/${testData.project.id}/translations", bearerHeaders(accessToken)).andIsUnauthorized
  }

  @Test
  fun `a grant survives a refresh when the client's metadata host answers a server error`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val code = driver.code(pending, projectId = null)
    val issued = json(driver.exchangeCode(code, clientId, redirect, pending.verifier))
    val grantId = stored(issued.get("access_token").asString()).id

    var refreshToken = issued.get("refresh_token").asString()
    listOf(500, 503, 429, 403).forEach { status ->
      documentStatus = status
      cimdClientCache.invalidate(clientId)

      val renewed = json(driver.refresh(refreshToken, clientId))
      renewed
        .get("access_token")
        .asString()
        .assert
        .isNotBlank()
      refreshToken = renewed.get("refresh_token").asString()
    }
    oauth2GrantRepository
      .findById(grantId)
      .get()
      .clientWithdrawnAt.assert
      .isNull()
  }

  @Test
  fun `a withdrawal another instance recorded is honoured by one whose cached answer predates it`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val code = driver.code(pending, projectId = null)
    val issued = json(driver.exchangeCode(code, clientId, redirect, pending.verifier))
    val grantId = stored(issued.get("access_token").asString()).id

    // The other instance saw the document go and wrote the mark; this one still caches the answer from the
    // authorize hop. The mark on the row is what decides, not the cache.
    documentWithdrawn = true
    cimdClientLifecycle.recordClientWithdrawn(clientId)

    json(driver.refresh(issued.get("refresh_token").asString(), clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    oauth2GrantRepository
      .findById(grantId)
      .get()
      .clientWithdrawnAt.assert
      .isNotNull()
  }

  @Test
  fun `a document that answers again lifts a mark young enough to be a mis-deploy`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val issued = json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))
    val refreshToken = issued.get("refresh_token").asString()

    documentWithdrawn = true
    json(driver.refresh(refreshToken, clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")

    // Past the re-check interval, still inside the grace window: the mis-deploy is fixed and read again in time.
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.MINUTES.toMillis(20))
    documentWithdrawn = false

    json(driver.refresh(refreshToken, clientId))
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a retirement a later read has already confirmed survives republishing`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val issued = json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))
    val refreshToken = issued.get("refresh_token").asString()

    documentWithdrawn = true
    json(driver.refresh(refreshToken, clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    val grantId = oauth2GrantRepository.findAll().first { it.clientId == clientId }.id

    // A second read with the document still gone: the mark has now survived a read, so it is no longer a
    // mis-deploy we are waiting to see recover.
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.MINUTES.toMillis(20))
    json(driver.refresh(refreshToken, clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")

    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.HOURS.toMillis(2))
    documentWithdrawn = false

    json(driver.refresh(refreshToken, clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    oauth2GrantRepository
      .findById(grantId)
      .get()
      .clientWithdrawnAt.assert
      .isNotNull()
  }

  /** The age bound counts failed reads, not idle time: a user coming back after a long break is not signed out by a blip. */
  @Test
  fun `a grant idle for longer than the maximum age is not refused over a single failed read`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val issued = json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))
    val issuedAt = currentDateProvider.date

    // Twice the maximum age, and this refresh is the first time anything reads the document again.
    currentDateProvider.forcedDate = Date(issuedAt.time + TimeUnit.DAYS.toMillis(14))
    documentStatus = 503

    json(driver.refresh(issued.get("refresh_token").asString(), clientId))
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a refresh inside the interval does not read the document again`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val issued = json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))
    val readsBefore = documentRequests.get()

    val first = json(driver.refresh(issued.get("refresh_token").asString(), clientId))
    documentRequests.get().assert.isEqualTo(readsBefore + 1)

    val second = json(driver.refresh(first.get("refresh_token").asString(), clientId))
    documentRequests.get().assert.isEqualTo(readsBefore + 1)

    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.MINUTES.toMillis(20))
    json(
      driver.refresh(second.get("refresh_token").asString(), clientId),
    ).get("access_token").asString().assert.isNotBlank()
    documentRequests.get().assert.isEqualTo(readsBefore + 2)
  }

  /** The fetch is the one outbound call an existing grant can cause, so only its real holder may cause it. */
  @Test
  fun `a refresh token that matches no live grant reads no document`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))
    driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirectAt(port), pending.verifier)
    val readsBefore = documentRequests.get()

    json(driver.refresh("tgort_not-a-token-anyone-was-issued", clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")

    documentRequests.get().assert.isEqualTo(readsBefore)
  }

  @Test
  fun `a mark no check has happened since is still liftable after the wall-clock window`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val issued = json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))

    documentWithdrawn = true
    json(driver.refresh(issued.get("refresh_token").asString(), clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")

    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.HOURS.toMillis(4))
    documentWithdrawn = false

    json(driver.refresh(issued.get("refresh_token").asString(), clientId))
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `an account cannot hold more document-backed clients than the cap`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    oauth2Properties.cimdMaxClientsPerUser = 1
    val first = driver.startPendingConsent(jwt(), clientIdAt(port), redirectAt(port))
    driver.exchangeCode(driver.code(first, projectId = null), clientIdAt(port), redirectAt(port), first.verifier)

    val refused = json(driver.startAuthorization(jwt(), otherClientIdAt(port), redirectAt(port), validParams(null)))

    // RFC 6749 4.1.2.1: the authorize step reports its refusal to the client through the redirect, not as a body.
    refused
      .get("consentState")
      .isNull.assert
      .isTrue()
    refused
      .get("redirectUrl")
      .asString()
      .assert
      .contains("error=access_denied")

    // The cap is on document-backed clients only: a client the operator registered is not in that work list.
    json(
      driver.startAuthorization(jwt(), OAuth2Constants.CLI_CLIENT_ID, "http://127.0.0.1/callback", validParams(null)),
    ).get("consentState")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a client the publisher retired stops holding a slot in the cap`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    oauth2Properties.cimdMaxClientsPerUser = 1
    val retired = clientIdAt(port)
    val pending = driver.startPendingConsent(jwt(), retired, redirectAt(port))
    val issued =
      json(driver.exchangeCode(driver.code(pending, projectId = null), retired, redirectAt(port), pending.verifier))

    documentWithdrawn = true
    json(
      driver.refresh(issued.get("refresh_token").asString(), retired),
    ).get("error").asString().assert.isEqualTo("invalid_grant")

    json(driver.startAuthorization(jwt(), otherClientIdAt(port), redirectAt(port), validParams(null)))
      .get("consentState")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a client whose grants have all expired stops holding a slot in the cap`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    oauth2Properties.cimdMaxClientsPerUser = 1
    val expiring = clientIdAt(port)
    val pending = driver.startPendingConsent(jwt(), expiring, redirectAt(port))
    driver.exchangeCode(driver.code(pending, projectId = null), expiring, redirectAt(port), pending.verifier)

    // Past the life of every token the grant holds, with nothing having deleted the row yet.
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.DAYS.toMillis(400))

    json(driver.startAuthorization(jwt(), otherClientIdAt(port), redirectAt(port), validParams(null)))
      .get("consentState")
      .asString()
      .assert
      .isNotBlank()
  }

  /** What one refresh learns about the document is a fact about the client, so every grant of it reads the same. */
  @Test
  fun `one successful read clears the failing state for every grant of the client`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val issuedAt = currentDateProvider.date
    val (first, second) =
      listOf(1, 2).map {
        val pending = driver.startPendingConsent(jwt(), clientId, redirect)
        json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))
      }

    // The first grant's refresh finds the document unreadable and starts the clock.
    documentStatus = 503
    val firstRenewed = json(driver.refresh(first.get("refresh_token").asString(), clientId))
    firstRenewed
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()

    // Past the maximum age, the first grant's refresh reads the document fine again.
    currentDateProvider.forcedDate = Date(issuedAt.time + TimeUnit.DAYS.toMillis(8))
    documentStatus = null
    json(
      driver.refresh(firstRenewed.get("refresh_token").asString(), clientId),
    ).get("access_token").asString().assert.isNotBlank()

    // The second grant's refresh fails to read again: a fresh failure, not one eight days old, so it is forgiven.
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.MINUTES.toMillis(20))
    documentStatus = 503
    json(driver.refresh(second.get("refresh_token").asString(), clientId))
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a grant ends once its client's document has failed to read for longer than the maximum age`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val issued = json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))
    val issuedAt = currentDateProvider.date
    documentStatus = 503

    // The first failed read starts the clock; the grant is still fine.
    currentDateProvider.forcedDate = Date(issuedAt.time + TimeUnit.DAYS.toMillis(6))
    val renewed = json(driver.refresh(issued.get("refresh_token").asString(), clientId))
    renewed
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()

    // Two days of failing: forgiven.
    currentDateProvider.forcedDate = Date(issuedAt.time + TimeUnit.DAYS.toMillis(8))
    val renewedAgain = json(driver.refresh(renewed.get("refresh_token").asString(), clientId))
    renewedAgain
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()

    // Past the bound counted from the first failure: that is the publisher's document, not our own downtime.
    currentDateProvider.forcedDate = Date(issuedAt.time + TimeUnit.DAYS.toMillis(13) + TimeUnit.MINUTES.toMillis(2))
    json(driver.refresh(renewedAgain.get("refresh_token").asString(), clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
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

  /** Sorts before [clientIdAt], so the round order in the starvation test does not depend on the database. */
  private fun otherClientIdAt(port: Int) = "http://127.0.0.1:$port/broken-client"

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
    created.createContext("/broken-client") { exchange ->
      if (otherDocumentBroken) {
        exchange.sendResponseHeaders(503, -1)
        exchange.close()
        return@createContext
      }
      val body = validDocument(otherClientIdAt(port), port).toByteArray(Charsets.UTF_8)
      exchange.responseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.use { it.write(body) }
    }
    created.createContext("/client") { exchange ->
      documentRequests.incrementAndGet()
      val status = documentStatus ?: 404.takeIf { documentWithdrawn }
      if (status != null) {
        exchange.sendResponseHeaders(status, -1)
        exchange.close()
        return@createContext
      }
      val body = document(port).toByteArray(Charsets.UTF_8)
      exchange.responseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.use { it.write(body) }
    }
    created.start()
    server = created
    return port
  }
}
