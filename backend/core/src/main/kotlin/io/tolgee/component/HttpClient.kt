package io.tolgee.component

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class HttpClient(
  private val restTemplate: RestTemplate,
) {
  fun <T> requestForJson(
    url: String,
    body: Any,
    method: HttpMethod,
    result: Class<T>,
    headers: HttpHeaders = HttpHeaders(),
  ): T {
    val bodyJson = jacksonObjectMapper().writeValueAsString(body)
    headers.apply {
      contentType = MediaType.APPLICATION_JSON
    }

    val response =
      restTemplate.exchange(
        url,
        method,
        HttpEntity(bodyJson, headers),
        String::class.java,
      )

    if (result == Unit::class.java) {
      @Suppress("UNCHECKED_CAST")
      return Unit as T
    }

    return response.body.let { stringResponseBody ->
      jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(stringResponseBody, result)
    }
  }
}
