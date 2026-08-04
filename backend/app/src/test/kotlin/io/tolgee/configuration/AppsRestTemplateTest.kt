package io.tolgee.configuration

import com.sun.net.httpserver.HttpServer
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.testing.assert
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.net.InetSocketAddress
import java.net.URI
import java.net.UnknownHostException

/** MockRestServiceServer replaces the HTTP client, so only a real connection can pin its config. */
class AppsRestTemplateTest {
  private lateinit var server: HttpServer
  private lateinit var restTemplate: RestTemplate

  private val baseUrl: String
    get() = "http://127.0.0.1:${server.address.port}"

  @BeforeEach
  fun setup() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/redirect") { exchange ->
      exchange.responseHeaders.add("Location", "http://169.254.169.254/latest/meta-data/")
      exchange.sendResponseHeaders(302, -1)
      exchange.close()
    }
    server.createContext("/manifest.json") { exchange ->
      val body = """{"id":"a"}""".toByteArray()
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.use { it.write(body) }
    }
    server.start()

    restTemplate =
      RestTemplateConfiguration().appsRestTemplate(
        UrlSecurity(InternalProperties()),
        AppsProperties().apply { allowLocalAddresses = true },
      )
  }

  @AfterEach
  fun tearDown() {
    server.stop(0)
  }

  @Test
  fun `does not follow a redirect toward a blocked address`() {
    val response = restTemplate.getForEntity(URI("$baseUrl/redirect"), String::class.java)

    response.statusCode
      .value()
      .assert
      .isEqualTo(302)
    response.headers
      .getFirst("Location")
      .assert
      .isEqualTo("http://169.254.169.254/latest/meta-data/")
  }

  @Test
  fun `fetches a normal manifest response`() {
    val body = restTemplate.getForObject(URI("$baseUrl/manifest.json"), String::class.java)
    body.assert.isEqualTo("""{"id":"a"}""")
  }

  @Test
  fun `refuses to connect to a host resolving to a blocked address`() {
    val blocking =
      RestTemplateConfiguration().appsRestTemplate(
        UrlSecurity(InternalProperties()),
        AppsProperties().apply { allowLocalAddresses = false },
      )

    val exception =
      assertThrows<ResourceAccessException> {
        blocking.getForObject(URI("$baseUrl/manifest.json"), String::class.java)
      }
    exception.cause.assert.isInstanceOf(UnknownHostException::class.java)
  }
}
