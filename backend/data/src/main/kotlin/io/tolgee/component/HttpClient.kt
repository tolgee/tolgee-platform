package io.tolgee.component

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper

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
      jacksonMapperBuilder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()
        .readValue(stringResponseBody, result)
    }
  }
}
