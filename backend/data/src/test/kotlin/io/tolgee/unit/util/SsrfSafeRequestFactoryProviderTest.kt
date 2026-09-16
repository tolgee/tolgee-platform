package io.tolgee.unit.util

import com.sun.net.httpserver.HttpServer
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.testing.assert
import io.tolgee.util.SsrfSafeRequestFactoryProvider
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration

class SsrfSafeRequestFactoryProviderTest {
  private val provider = SsrfSafeRequestFactoryProvider(UrlSecurity(InternalProperties()))
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

    // The validating DNS resolver rejects the loopback address before any connection, so the request never lands.
    assertThrows<RestClientException> { restTemplate.getForObject("http://127.0.0.1:$port/", String::class.java) }
  }

  @Test
  fun `a template that allows local addresses reaches the loopback server`() {
    val restTemplate =
      RestTemplate(provider.create(allowLocalAddresses = true, connectTimeout = SHORT, responseTimeout = SHORT))

    restTemplate.getForObject("http://127.0.0.1:$port/", String::class.java).assert.isEqualTo("ok")
  }

  companion object {
    private val SHORT = Duration.ofSeconds(2)
  }
}
