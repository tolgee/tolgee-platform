package io.tolgee.security.oauth2.cimd

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tolgee.Metrics
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.testing.assert
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration

class CimdDocumentFetcherTest {
  private val fetcher =
    CimdDocumentFetcher(
      CimdHostResolver(UrlSecurity(InternalProperties()), InternalProperties(), Metrics(SimpleMeterRegistry())),
      InternalProperties(),
    )
  private var server: HttpServer? = null

  @AfterEach
  fun stopServer() {
    server?.stop(0)
    server = null
  }

  @Test
  fun `the connection goes to the pinned address, not the URL host's own resolution`() {
    val port = serve { it.respond(200, """{"ok":true}""") }

    // A .invalid host never resolves through real DNS, so a body coming back proves the pin — not a lookup — chose the
    // address. The connection lands on the loopback the server listens on only because that is what we pinned.
    val onListeningAddress = fetcher.fetchPinned(url(port), listOf(loopback()), DEADLINE)
    onListeningAddress.assert.isEqualTo(CimdDocument.Body("""{"ok":true}"""))
  }

  @Test
  fun `pinning to a different address than the one listening fails rather than falling back`() {
    val port = serve { it.respond(200, """{"ok":true}""") }

    // Same URL, same real listener, but pinned to a loopback address where nothing listens: a re-resolving client
    // would still reach the server, a pinned one must not.
    val onWrongAddress = fetcher.fetchPinned(url(port), listOf(InetAddress.getByName("127.0.0.2")), DEADLINE)
    onWrongAddress.assert.isEqualTo(CimdDocument.Unavailable)
  }

  @Test
  fun `a byte-dripping server is abandoned at the deadline instead of held forever`() {
    val port =
      serve { exchange ->
        exchange.sendResponseHeaders(200, 0)
        val out = exchange.responseBody
        repeat(500) {
          runCatching {
            out.write('.'.code)
            out.flush()
            Thread.sleep(200)
          }.onFailure { return@serve }
        }
      }

    val start = System.nanoTime()
    val result = fetcher.fetchPinned(url(port), listOf(loopback()), Duration.ofSeconds(1))
    val elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis()

    result.assert.isEqualTo(CimdDocument.Unavailable)
    elapsedMillis.assert
      .withFailMessage("the drip should have been cut near the 1s deadline, took %sms", elapsedMillis)
      .isLessThan(10_000)
  }

  @Test
  fun `a body over the size cap is refused rather than buffered whole`() {
    val port = serve { it.respond(200, "x".repeat(4096)) }

    fetcher
      .fetchPinned(url(port), listOf(loopback()), DEADLINE, maxBytes = 1024)
      .assert
      .isEqualTo(CimdDocument.Unavailable)
  }

  @Test
  fun `a body at the size cap is returned`() {
    val body = "y".repeat(1024)
    val port = serve { it.respond(200, body) }

    fetcher
      .fetchPinned(url(port), listOf(loopback()), DEADLINE, maxBytes = 1024)
      .assert
      .isEqualTo(CimdDocument.Body(body))
  }

  @Test
  fun `a redirect is not followed`() {
    val port =
      serve { exchange ->
        exchange.responseHeaders.add("Location", "https://elsewhere.example/other")
        exchange.sendResponseHeaders(302, -1)
        exchange.close()
      }

    fetcher.fetchPinned(url(port), listOf(loopback()), DEADLINE).assert.isEqualTo(CimdDocument.Unavailable)
  }

  @Test
  fun `a document that is not served as JSON is refused`() {
    val port = serve { it.respond(200, """{"ok":true}""", contentType = "text/html") }

    fetcher.fetchPinned(url(port), listOf(loopback()), DEADLINE).assert.isEqualTo(CimdDocument.Rejected)
  }

  @Test
  fun `a JSON media type with parameters or a structured suffix is accepted`() {
    val port = serve { it.respond(200, """{"ok":true}""", contentType = "application/json; charset=utf-8") }

    fetcher.fetchPinned(url(port), listOf(loopback()), DEADLINE).assert.isEqualTo(CimdDocument.Body("""{"ok":true}"""))
  }

  @Test
  fun `a 404 or a 410 is the host saying the document is gone`() {
    listOf(404, 410).forEach { status ->
      val port = serve { it.respond(status, "nope") }

      fetcher.fetchPinned(url(port), listOf(loopback()), DEADLINE).assert.isEqualTo(CimdDocument.Gone)
    }
  }

  @Test
  fun `a server error or a throttle is a non-answer, not a verdict about the document`() {
    listOf(403, 429, 500, 502, 503).forEach { status ->
      val port = serve { it.respond(status, "nope") }

      fetcher.fetchPinned(url(port), listOf(loopback()), DEADLINE).assert.isEqualTo(CimdDocument.Unavailable)
    }
  }

  @Test
  fun `a client_id longer than the grant column is refused before any fetch`() {
    val tooLong = "https://a.example/" + "p".repeat(CimdUrls.MAX_CLIENT_ID_LENGTH)

    fetcher.fetch(tooLong).assert.isEqualTo(CimdDocument.Rejected)
  }

  @Test
  fun `a non-https client_id is refused before any fetch`() {
    fetcher.fetch("http://example.com/.well-known/client").assert.isEqualTo(CimdDocument.Rejected)
    fetcher.fetch("not a url").assert.isEqualTo(CimdDocument.Rejected)
  }

  @Test
  fun `an unreachable host reads as unavailable, not as a verdict about the client`() {
    fetcher.fetch("https://no-such-host.invalid/.well-known/client").assert.isEqualTo(CimdDocument.Unavailable)
  }

  @Test
  fun `a client_id resolving to a blocked address is refused, never a 500`() {
    fetcher.fetch("https://127.0.0.1/.well-known/client").assert.isEqualTo(CimdDocument.Unavailable)
    fetcher.fetch("https://10.0.0.1/.well-known/client").assert.isEqualTo(CimdDocument.Unavailable)
    fetcher.fetch("https://[fd00::1]/.well-known/client").assert.isEqualTo(CimdDocument.Unavailable)
  }

  private fun HttpExchange.respond(
    status: Int,
    body: String,
    contentType: String? = "application/json",
  ) {
    val bytes = body.toByteArray(Charsets.UTF_8)
    contentType?.let { responseHeaders.add("Content-Type", it) }
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.use { it.write(bytes) }
  }

  private fun serve(handler: (HttpExchange) -> Unit): Int {
    val created = HttpServer.create(InetSocketAddress(loopback(), 0), 0)
    created.createContext("/") { exchange -> exchange.use { handler(it) } }
    created.executor = null
    created.start()
    server = created
    return created.address.port
  }

  private fun loopback(): InetAddress = InetAddress.getByName("127.0.0.1")

  private fun url(port: Int) = "http://cimd.invalid:$port/.well-known/client"

  companion object {
    private val DEADLINE = Duration.ofSeconds(5)
  }
}
