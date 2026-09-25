package io.tolgee.configuration

import com.sun.net.httpserver.HttpServer
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.configuration.tolgee.WebhookProperties
import io.tolgee.testing.assert
import io.tolgee.util.SsrfSafeRequestFactoryProvider
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClientException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class SsrfSafeRestTemplateBeansTest {
  private val internalProperties = InternalProperties()
  private val properties = TolgeeProperties()
  private val webhookProperties = WebhookProperties()
  private val configuration =
    RestTemplateConfiguration(
      SsrfSafeRequestFactoryProvider(UrlSecurity(internalProperties), internalProperties),
      webhookProperties,
      properties,
    )

  private lateinit var server: HttpServer
  private var port = 0

  @BeforeEach
  fun serveOk() {
    server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
    server.createContext("/") { exchange ->
      exchange.responseHeaders.add("Set-Cookie", "tenant=a; Path=/")
      val bytes = "ok".toByteArray()
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    port = server.address.port
  }

  @AfterEach
  fun stop() {
    server.stop(0)
  }

  @Test
  fun `global SSO reaches an operator's IdP on a private address`() {
    configuration
      .ssoGlobalRestTemplate()
      .getForObject("http://127.0.0.1:$port/", String::class.java)
      .assert
      .isEqualTo("ok")
  }

  @Test
  fun `an organization's own SSO tenant is still held to the address block`() {
    properties.authentication.ssoOrganizations.allowLocalAddresses = false

    assertThrows<RestClientException> {
      configuration.ssoRestTemplate().getForObject("http://127.0.0.1:$port/", String::class.java)
    }
  }

  @Test
  fun `no template that carries a secret follows a redirect`() {
    server.createContext("/hop") { exchange ->
      exchange.responseHeaders.add("Location", "http://127.0.0.1:$port/")
      exchange.sendResponseHeaders(302, -1)
      exchange.close()
    }
    webhookProperties.allowLocalAddresses = true
    properties.authentication.ssoOrganizations.allowLocalAddresses = true

    listOf(
      configuration.ssoGlobalRestTemplate(),
      configuration.ssoRestTemplate(),
      configuration.webhookRestTemplate(),
    ).forEach { template ->
      // The 302 is handed back as the answer, so the caller sees the redirect rather than its target's body.
      template.getForObject("http://127.0.0.1:$port/hop", String::class.java).assert.isNull()
    }
  }

  @Test
  fun `a 503 naming a long Retry-After is handed back at once rather than slept on`() {
    val requests = AtomicInteger()
    server.createContext("/throttled") { exchange ->
      requests.incrementAndGet()
      exchange.responseHeaders.add("Retry-After", "86400")
      exchange.sendResponseHeaders(503, -1)
      exchange.close()
    }
    val template = configuration.ssoGlobalRestTemplate()

    val start = System.nanoTime()
    assertThrows<HttpServerErrorException> {
      template.getForObject("http://127.0.0.1:$port/throttled", String::class.java)
    }
    val elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis()

    // The request count is the signal: a retry would show up here whatever the machine's timing looks like.
    requests.get().assert.isEqualTo(1)
    elapsedMillis.assert
      .withFailMessage("the 503 should have come straight back, took %sms", elapsedMillis)
      .isLessThan(10_000)
  }

  @Test
  fun `a cookie one caller is handed is not carried onto the next caller's request`() {
    val template = configuration.ssoGlobalRestTemplate()
    template.getForObject("http://127.0.0.1:$port/", String::class.java)

    val sent = mutableListOf<String>()
    server.createContext("/echo") { exchange ->
      sent += exchange.requestHeaders.getFirst("Cookie").orEmpty()
      exchange.sendResponseHeaders(200, 0)
      exchange.close()
    }
    template.getForObject("http://127.0.0.1:$port/echo", String::class.java)

    sent.single().assert.isEmpty()
  }
}
