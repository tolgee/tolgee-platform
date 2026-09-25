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

  @Autowired
  private lateinit var documentCheck: OAuth2CimdDocumentCheck

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

  /** A second client on the same host, so one client's document can break while the other keeps serving. */
  private var otherDocumentBroken: Boolean = false

  @AfterEach
  fun resetFixture() {
    stopServer()
    extraRedirectUri = null
    documentWithdrawn = false
    documentStatus = null
    otherDocumentBroken = false
    oauth2Properties.cimdCheckBatchSize = OAuth2ServerProperties().cimdCheckBatchSize
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
    documentCheck.checkBatch()

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
  }

  @Test
  fun `a withdrawal the check observes is written to the grant, so the access token dies with it`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))
    val code = driver.code(pending, projectId = testData.project.id)
    val tokens = json(driver.exchangeCode(code, clientId, redirectAt(port), pending.verifier))
    val accessToken = tokens.get("access_token").asString()
    performGet("/v2/projects/${testData.project.id}/translations", bearerHeaders(accessToken)).andIsOk

    documentWithdrawn = true
    documentCheck.checkBatch()

    json(driver.refresh(tokens.get("refresh_token").asString(), clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")

    stored(accessToken).clientWithdrawnAt.assert.isNotNull()
    cimdClientCache.invalidate(clientId)
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
    grantRepository
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

    cimdClientLifecycle.recordClientWithdrawn(clientId)

    json(driver.refresh(issued.get("refresh_token").asString(), clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    grantRepository
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
    documentCheck.checkBatch()

    // Past the re-check interval, still inside the grace window: the mis-deploy is fixed and read again in time.
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.MINUTES.toMillis(20))
    documentWithdrawn = false
    documentCheck.checkBatch()

    json(driver.refresh(refreshToken, clientId))
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a retirement the check has already confirmed survives republishing`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val issued = json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))
    val refreshToken = issued.get("refresh_token").asString()

    documentWithdrawn = true
    documentCheck.checkBatch()
    val grantId = grantRepository.findAll().first { it.clientId == clientId }.id

    // A second round with the document still gone: the mark has now survived a read, so it is no longer a
    // mis-deploy we are waiting to see recover.
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.MINUTES.toMillis(20))
    documentCheck.checkBatch()

    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.HOURS.toMillis(2))
    documentWithdrawn = false
    documentCheck.checkBatch()
    cimdClientCache.invalidate(clientId)

    json(driver.refresh(refreshToken, clientId))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    grantRepository
      .findById(grantId)
      .get()
      .clientWithdrawnAt.assert
      .isNotNull()
  }

  @Test
  fun `a client whose document cannot be read does not keep the others from being checked`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val broken = otherClientIdAt(port)
    val healthy = clientIdAt(port)
    // The broken one first, so it is also the one an ordering by last-success would keep picking.
    listOf(broken, healthy).forEach { clientId ->
      val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))
      driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirectAt(port), pending.verifier)
    }
    otherDocumentBroken = true
    oauth2Properties.cimdCheckBatchSize = 1

    repeat(cimdClientLifecycle.clientIdsDueForCheck(1000).size) { documentCheck.checkBatch() }

    grantRepository
      .findAll()
      .first { it.clientId == healthy }
      .cimdVerifiedAt.assert
      .isNotNull()
  }

  @Test
  fun `a client read a moment ago is not read again in the same interval`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))
    driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirectAt(port), pending.verifier)

    documentCheck.checkBatch()

    cimdClientLifecycle.clientIdsDueForCheck(1000).assert.doesNotContain(clientId)
  }

  @Test
  fun `a client retired for good is not read any more`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))
    driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirectAt(port), pending.verifier)

    documentWithdrawn = true
    documentCheck.checkBatch()
    // One more round with the document still gone, so the mark has had its chance to be lifted and was not.
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.MINUTES.toMillis(20))
    documentCheck.checkBatch()
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.HOURS.toMillis(2))

    cimdClientLifecycle.clientIdsDueForCheck(1000).assert.doesNotContain(clientId)
  }

  @Test
  fun `a grant is kept when nothing has tried to read its document, however old the last read is`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val issued = json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))
    val issuedAt = currentDateProvider.date

    // Twice the maximum age, and the check has never run.
    currentDateProvider.forcedDate = Date(issuedAt.time + TimeUnit.DAYS.toMillis(14))
    documentStatus = 503
    cimdClientCache.invalidate(clientId)

    json(driver.refresh(issued.get("refresh_token").asString(), clientId))
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a mark no check has happened since is still liftable after the wall-clock window`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val issued = json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))

    documentWithdrawn = true
    documentCheck.checkBatch()

    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.HOURS.toMillis(4))
    documentWithdrawn = false
    documentCheck.checkBatch()

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
    driver.exchangeCode(driver.code(pending, projectId = null), retired, redirectAt(port), pending.verifier)

    documentWithdrawn = true
    documentCheck.checkBatch()

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

  @Test
  fun `a freshly onboarded client does not jump ahead of one already waiting`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val waiting = clientIdAt(port)
    val first = driver.startPendingConsent(jwt(), waiting, redirectAt(port))
    driver.exchangeCode(driver.code(first, projectId = null), waiting, redirectAt(port), first.verifier)
    documentCheck.checkBatch()

    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.MINUTES.toMillis(20))
    val fresh = otherClientIdAt(port)
    val second = driver.startPendingConsent(jwt(), fresh, redirectAt(port))
    driver.exchangeCode(driver.code(second, projectId = null), fresh, redirectAt(port), second.verifier)

    cimdClientLifecycle.clientIdsDueForCheck(1).assert.containsExactly(waiting)
  }

  /**
   * The backlog gauge runs a hand-written native copy of the work-list predicate, and the two are only equal by
   * hand. A divergence shows up as a permanently wrong gauge and nothing else, because the job logs and swallows
   * any error from it - and that gauge is the only signal that retirements and the freshness bound are keeping up.
   */
  @Test
  fun `the backlog gauge counts exactly the clients the work list would return`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val listed = { cimdClientLifecycle.clientIdsDueForCheck(1000).size.toLong() }
    val counted = { cimdClientLifecycle.clientsDueForCheckCount() }

    counted().assert.isEqualTo(listed())

    listOf(clientIdAt(port), otherClientIdAt(port)).forEach { clientId ->
      val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))
      driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirectAt(port), pending.verifier)
    }
    counted().assert.isEqualTo(listed())

    // Read once: both sides have to apply the interval the same way.
    documentCheck.checkBatch()
    counted().assert.isEqualTo(listed())

    // Past the interval, so both sides have to bring them back.
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.MINUTES.toMillis(60))
    counted().assert.isEqualTo(listed())
    counted().assert.isEqualTo(2L)

    // A fresh withdrawal, inside the grace window: both sides have to keep the client, which is still liftable.
    documentWithdrawn = true
    documentCheck.checkBatch()
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.MINUTES.toMillis(20))
    counted().assert.isEqualTo(listed())
    cimdClientLifecycle.clientIdsDueForCheck(1000).assert.contains(clientIdAt(port))

    // Past the wall-clock window, but the mark is still newer than the attempt before it. Only the third measure
    // keeps the client now, and that is the one the two queries spell differently.
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.HOURS.toMillis(4))
    counted().assert.isEqualTo(listed())
    cimdClientLifecycle.clientIdsDueForCheck(1000).assert.contains(clientIdAt(port))
    counted().assert.isEqualTo(2L)

    // A second attempt with the document still gone, then past the window again: retired for good, and only the
    // client nobody withdrew is left.
    documentCheck.checkBatch()
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + TimeUnit.HOURS.toMillis(2))
    counted().assert.isEqualTo(listed())
    cimdClientLifecycle.clientIdsDueForCheck(1000).assert.doesNotContain(clientIdAt(port))
    counted().assert.isEqualTo(1L)
  }

  @Test
  fun `a round takes no more clients than its batch size`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    listOf(clientIdAt(port), otherClientIdAt(port)).forEach { clientId ->
      val pending = driver.startPendingConsent(jwt(), clientId, redirectAt(port))
      driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirectAt(port), pending.verifier)
    }

    cimdClientLifecycle.clientIdsDueForCheck(1).assert.hasSize(1)
  }

  @Test
  fun `one read keeps every grant of that client alive, not only the one that triggered it`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val issuedAt = currentDateProvider.date
    val untouched =
      listOf(1, 2).map {
        val pending = driver.startPendingConsent(jwt(), clientId, redirect)
        json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))
      }

    currentDateProvider.forcedDate = Date(issuedAt.time + TimeUnit.DAYS.toMillis(6))
    documentCheck.checkBatch()

    // Past the maximum age counted from when the grants were made, but well inside it counted from that one read.
    currentDateProvider.forcedDate = Date(issuedAt.time + TimeUnit.DAYS.toMillis(10))
    documentStatus = 503
    cimdClientCache.invalidate(clientId)

    untouched.forEach { issued ->
      json(driver.refresh(issued.get("refresh_token").asString(), clientId))
        .get("access_token")
        .asString()
        .assert
        .isNotBlank()
    }
  }

  @Test
  fun `a grant ends once its client's document has been unreadable for longer than the maximum age`() {
    val port = serveDocument { p -> validDocument(clientIdAt(p), p) }
    val clientId = clientIdAt(port)
    val redirect = redirectAt(port)
    val pending = driver.startPendingConsent(jwt(), clientId, redirect)
    val issued = json(driver.exchangeCode(driver.code(pending, projectId = null), clientId, redirect, pending.verifier))
    val issuedAt = currentDateProvider.date
    documentStatus = 503

    currentDateProvider.forcedDate = Date(issuedAt.time + TimeUnit.DAYS.toMillis(6))
    documentCheck.checkBatch()
    cimdClientCache.invalidate(clientId)
    val renewed = json(driver.refresh(issued.get("refresh_token").asString(), clientId))
    renewed
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()

    // The check keeps trying and keeps failing: that is the publisher's document, not our own downtime.
    currentDateProvider.forcedDate = Date(issuedAt.time + TimeUnit.DAYS.toMillis(8))
    documentCheck.checkBatch()
    cimdClientCache.invalidate(clientId)

    json(driver.refresh(renewed.get("refresh_token").asString(), clientId))
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
