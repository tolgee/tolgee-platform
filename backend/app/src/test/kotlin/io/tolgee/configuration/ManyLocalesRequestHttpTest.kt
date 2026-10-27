package io.tolgee.configuration

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Tomcat 11.0.27 rejects requests whose Accept-Language header holds more than
 * maxLocaleCount (default 10) locales when a handler reads the request locales.
 * Browsers of multilingual users send more, so such requests must still succeed.
 * MockMvc cannot cover this: only a real Tomcat parses the header.
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = [
    "tolgee.websocket.use-redis=false",
  ],
)
class ManyLocalesRequestHttpTest {
  @LocalServerPort
  var port: Int = 0

  private val client: HttpClient = HttpClient.newHttpClient()

  @Test
  fun `request with more than 10 locales in Accept-Language is not rejected`() {
    val fifteenLocales =
      "en-US,en;q=0.9,de-DE;q=0.8,de;q=0.8,fr-FR;q=0.7,fr;q=0.7,it-IT;q=0.6," +
        "it;q=0.6,es-ES;q=0.5,es;q=0.5,cs-CZ;q=0.4,cs;q=0.4,pl;q=0.3,ru;q=0.2,uk;q=0.1"

    val response = get(fifteenLocales)

    response.statusCode().assert.isEqualTo(HttpStatus.OK.value())
  }

  @Test
  fun `request with more locales than the raised cap is still rejected`() {
    val tooManyLocales =
      ('a'..'z')
        .flatMap { first -> ('a'..'z').map { second -> "$first$second" } }
        .take(TomcatMaxLocaleCountConfiguration.MAX_LOCALE_COUNT + 1)
        .joinToString(",")

    val response = get(tooManyLocales)

    response.statusCode().assert.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
  }

  private fun get(acceptLanguage: String): HttpResponse<String> {
    val request =
      HttpRequest
        .newBuilder(URI("http://localhost:$port/websocket/info"))
        .header("Accept-Language", acceptLanguage)
        .GET()
        .build()

    return client.send(request, HttpResponse.BodyHandlers.ofString())
  }
}
