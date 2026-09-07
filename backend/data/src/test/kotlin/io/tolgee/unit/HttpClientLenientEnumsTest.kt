package io.tolgee.unit

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import io.tolgee.component.HttpClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

/**
 * Leniency is opt-in: a licence server may be newer than this instance, but an identity provider
 * answering with something unexpected is something we want to hear about.
 */
class HttpClientLenientEnumsTest {
  enum class Metric {
    @JsonEnumDefaultValue
    KNOWN_FALLBACK,
    HOSTED_WORDS,
  }

  data class Response(
    val metric: Metric = Metric.HOSTED_WORDS,
  )

  @Test
  fun `an unknown enum value falls back when the caller asks for leniency`() {
    val result =
      clientAnswering("""{"metric":"HOSTED_CHARACTERS"}""")
        .requestForJson("url", "{}", HttpMethod.POST, Response::class.java, lenientEnums = true)

    assertThat(result.metric).isEqualTo(Metric.KNOWN_FALLBACK)
  }

  @Test
  fun `an unknown enum value still fails by default`() {
    val client = clientAnswering("""{"metric":"HOSTED_CHARACTERS"}""")

    assertThatThrownBy {
      client.requestForJson("url", "{}", HttpMethod.POST, Response::class.java)
    }.isInstanceOf(Exception::class.java)
  }

  @Test
  fun `a known value deserializes either way`() {
    val result =
      clientAnswering("""{"metric":"HOSTED_WORDS"}""")
        .requestForJson("url", "{}", HttpMethod.POST, Response::class.java, lenientEnums = true)

    assertThat(result.metric).isEqualTo(Metric.HOSTED_WORDS)
  }

  private fun clientAnswering(body: String): HttpClient {
    val restTemplate =
      mock<RestTemplate> {
        on {
          exchange(any<String>(), any(), any<HttpEntity<*>>(), any<Class<String>>())
        } doReturn ResponseEntity.ok(body)
      }
    return HttpClient(restTemplate)
  }
}
