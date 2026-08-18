package io.tolgee.ee.unit

import io.tolgee.configuration.WebConfiguration
import org.mockito.kotlin.mock
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

/**
 * Builds a [RestTemplate] answering every request with [responseJson], deserializing through the
 * application's own `JsonMapper` — the LLM services get theirs from the Spring-configured
 * `RestTemplateBuilder`, so a stub on plain defaults would not exercise the same Jackson settings.
 */
fun stubLlmRestTemplate(responseJson: String): RestTemplate {
  val factory =
    ClientHttpRequestFactory { uri, httpMethod ->
      StubClientHttpRequest(uri, httpMethod, responseJson)
    }

  val restTemplate = RestTemplate(factory)
  restTemplate.messageConverters.replaceAll {
    if (it is JacksonJsonHttpMessageConverter) JacksonJsonHttpMessageConverter(applicationJsonMapper) else it
  }
  return restTemplate
}

private val applicationJsonMapper = WebConfiguration(mock(), mock()).objectMapper()

private class StubClientHttpRequest(
  private val uri: URI,
  private val httpMethod: HttpMethod,
  private val responseJson: String,
) : ClientHttpRequest {
  private val outputStream = ByteArrayOutputStream()
  private val headers = HttpHeaders()

  override fun getMethod() = httpMethod

  override fun getURI() = uri

  override fun getHeaders() = headers

  override fun getBody(): OutputStream = outputStream

  override fun getAttributes(): MutableMap<String, Any> = mutableMapOf()

  override fun execute(): ClientHttpResponse = StubClientHttpResponse(responseJson)
}

private class StubClientHttpResponse(
  private val body: String,
) : ClientHttpResponse {
  private val headers =
    HttpHeaders().apply {
      contentType = MediaType.APPLICATION_JSON
    }

  override fun getStatusCode() = HttpStatus.OK

  override fun getHeaders() = headers

  override fun getBody(): InputStream = ByteArrayInputStream(body.toByteArray())

  override fun close() {}

  @Deprecated("Deprecated in Java")
  override fun getStatusText() = "OK"
}
