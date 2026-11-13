package io.tolgee.unit.util

import com.sun.net.httpserver.HttpServer
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.testing.assert
import io.tolgee.util.SsrfSafeRequestFactoryProvider
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class SsrfSafeRequestFactoryProviderTest {
  private val internalProperties = InternalProperties()
  private val provider = SsrfSafeRequestFactoryProvider(UrlSecurity(internalProperties), internalProperties)
  private lateinit var server: HttpServer
  private var port = 0

  @BeforeEach
  fun serveOk() {
    server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
    server.createContext("/") { exchange ->
      val bytes = "ok".toByteArray()
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    // A thread per concurrent request, or the parked-request test would serialise and never reach the pool cap.
    server.executor = Executors.newCachedThreadPool()
    server.start()
    port = server.address.port
  }

  @AfterEach
  fun stop() {
    server.stop(0)
  }

  @Test
  fun `a template built for public traffic refuses to connect to a loopback host`() {
    val restTemplate =
      RestTemplate(provider.create(allowLocalAddresses = false, connectTimeout = SHORT, responseTimeout = SHORT))

    assertThrows<RestClientException> { restTemplate.getForObject("http://127.0.0.1:$port/", String::class.java) }
  }

  @Test
  fun `a template that allows local addresses reaches the loopback server`() {
    val restTemplate =
      RestTemplate(provider.create(allowLocalAddresses = true, connectTimeout = SHORT, responseTimeout = SHORT))

    restTemplate.getForObject("http://127.0.0.1:$port/", String::class.java).assert.isEqualTo("ok")
  }

  @Test
  fun `a caller that opts into redirects still has every hop re-vetted by the resolver`() {
    serveRedirectToLoopback()
    val urlSecurity =
      mock<UrlSecurity> {
        on { resolveAndValidateHost(eq("localhost"), any()) } doReturn listOf(InetAddress.getByName("127.0.0.1"))
        on { resolveAndValidateHost(eq("127.0.0.1"), any()) } doThrow BadRequestException(Message.URL_NOT_VALID)
      }
    val factory =
      providerWith(urlSecurity).create(
        allowLocalAddresses = true,
        connectTimeout = SHORT,
        responseTimeout = SHORT,
        followRedirects = true,
      )

    assertThrows<RestClientException> {
      RestTemplate(factory).getForObject("http://localhost:$port/hop", String::class.java)
    }
  }

  @Test
  fun `a caller that opts into redirects reaches a hop the resolver accepts`() {
    serveRedirectToLoopback()
    val template =
      RestTemplate(
        provider.create(
          allowLocalAddresses = true,
          connectTimeout = SHORT,
          responseTimeout = SHORT,
          followRedirects = true,
        ),
      )

    template.getForObject("http://localhost:$port/hop", String::class.java).assert.isEqualTo("ok")
  }

  @Test
  fun `a caller that did not opt into redirects is handed the 302 instead of following it`() {
    serveRedirectToLoopback()
    val restTemplate =
      RestTemplate(provider.create(allowLocalAddresses = true, connectTimeout = SHORT, responseTimeout = SHORT))

    val response = restTemplate.getForEntity("http://127.0.0.1:$port/hop", String::class.java)

    response.statusCode
      .value()
      .assert
      .isEqualTo(302)
    response.body.assert.isNull()
  }

  @Test
  fun `one configuration yields one shared client rather than a fresh pool per call`() {
    val first = provider.create(allowLocalAddresses = true, connectTimeout = SHORT, responseTimeout = SHORT)
    val second = provider.create(allowLocalAddresses = true, connectTimeout = SHORT, responseTimeout = SHORT)
    val other = provider.create(allowLocalAddresses = false, connectTimeout = SHORT, responseTimeout = SHORT)

    (first as HttpComponentsClientHttpRequestFactory)
      .httpClient.assert
      .isSameAs((second as HttpComponentsClientHttpRequestFactory).httpClient)
    first.httpClient.assert.isNotSameAs((other as HttpComponentsClientHttpRequestFactory).httpClient)
    provider.pooledClientCount().assert.isEqualTo(2)
  }

  @Test
  fun `each configuration still enforces its own address policy`() {
    val local = provider.create(allowLocalAddresses = true, connectTimeout = SHORT, responseTimeout = SHORT)
    val public = provider.create(allowLocalAddresses = false, connectTimeout = SHORT, responseTimeout = SHORT)

    RestTemplate(local).getForObject("http://127.0.0.1:$port/", String::class.java).assert.isEqualTo("ok")
    assertThrows<RestClientException> {
      RestTemplate(public).getForObject("http://127.0.0.1:$port/", String::class.java)
    }
  }

  @Test
  fun `more than httpclient5's five-per-route default reach one host at once`() {
    val concurrent = 12
    val release = CountDownLatch(1)
    val arrived = CountDownLatch(concurrent)
    server.createContext("/park") { exchange ->
      arrived.countDown()
      release.await()
      exchange.sendResponseHeaders(200, 0)
      exchange.close()
    }
    val template =
      RestTemplate(provider.create(allowLocalAddresses = true, connectTimeout = LONG, responseTimeout = LONG))
    val pool = Executors.newFixedThreadPool(concurrent)
    val inFlight =
      (1..concurrent).map { pool.submit { template.getForObject("http://127.0.0.1:$port/park", String::class.java) } }

    arrived.await(20, TimeUnit.SECONDS).assert.isTrue()

    release.countDown()
    inFlight.forEach { it.get() }
    pool.shutdown()
  }

  @Test
  fun `a local address is reachable when SSRF protection is disabled for dev, matching create-time validation`() {
    val devProperties = InternalProperties().apply { disableUrlSsrfProtection = true }
    val devProvider = SsrfSafeRequestFactoryProvider(UrlSecurity(devProperties), devProperties)
    val restTemplate =
      RestTemplate(devProvider.create(allowLocalAddresses = false, connectTimeout = SHORT, responseTimeout = SHORT))

    restTemplate.getForObject("http://127.0.0.1:$port/", String::class.java).assert.isEqualTo("ok")
  }

  @Test
  fun `a request goes out through the operator's proxy, though the proxy's own address is private`() {
    ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { proxy ->
      val requestLine = answerOneRequest(proxy)
      withProxyProperties(host = "127.0.0.1", port = proxy.localPort) {
        val restTemplate =
          RestTemplate(provider.create(allowLocalAddresses = false, connectTimeout = SHORT, responseTimeout = SHORT))

        restTemplate.getForObject("http://webhook.invalid/hook", String::class.java).assert.isEqualTo("ok")
      }
      requestLine.get(10, TimeUnit.SECONDS).assert.contains("http://webhook.invalid/hook")
    }
  }

  @Test
  fun `a private host that is not the proxy stays refused while a proxy is configured`() {
    ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { proxy ->
      withProxyProperties(host = "127.0.0.1", port = proxy.localPort) {
        val restTemplate =
          RestTemplate(provider.create(allowLocalAddresses = false, connectTimeout = SHORT, responseTimeout = SHORT))

        // The JVM's default `http.nonProxyHosts` covers loopback, so this one goes out past the proxy.
        assertThrows<RestClientException> { restTemplate.getForObject("http://localhost:$port/", String::class.java) }
      }
    }
  }

  @Test
  fun `a direct request to the proxy's own host name is refused like any other private host`() {
    ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { proxy ->
      withProxyProperties(host = "127.0.0.1", port = proxy.localPort) {
        val restTemplate =
          RestTemplate(provider.create(allowLocalAddresses = false, connectTimeout = SHORT, responseTimeout = SHORT))

        // The JVM's default `http.nonProxyHosts` covers 127.*, so this one never reaches the proxy.
        assertThrows<RestClientException> { restTemplate.getForObject("http://127.0.0.1:$port/", String::class.java) }
      }
    }
  }

  private fun <T> withProxyProperties(
    host: String,
    port: Int,
    block: () -> T,
  ): T {
    System.setProperty("http.proxyHost", host)
    System.setProperty("http.proxyPort", port.toString())
    return try {
      block()
    } finally {
      System.clearProperty("http.proxyHost")
      System.clearProperty("http.proxyPort")
    }
  }

  /** Answers one proxied request and hands back the request line, which names the target the proxy was asked for. */
  private fun answerOneRequest(serverSocket: ServerSocket): Future<String> {
    val executor = Executors.newSingleThreadExecutor()
    return executor
      .submit<String> {
        serverSocket.accept().use { socket ->
          val reader = socket.getInputStream().bufferedReader()
          val requestLine = reader.readLine()
          var header = reader.readLine()
          while (!header.isNullOrEmpty()) header = reader.readLine()
          socket.getOutputStream().apply {
            write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".toByteArray())
            flush()
          }
          requestLine
        }
      }.also { executor.shutdown() }
  }

  private fun serveRedirectToLoopback() {
    server.createContext("/hop") { exchange ->
      exchange.responseHeaders.add("Location", "http://127.0.0.1:$port/")
      exchange.sendResponseHeaders(302, -1)
      exchange.close()
    }
  }

  private fun providerWith(urlSecurity: UrlSecurity) = SsrfSafeRequestFactoryProvider(urlSecurity, internalProperties)

  companion object {
    private val SHORT = Duration.ofSeconds(2)
    private val LONG = Duration.ofSeconds(30)
  }
}
