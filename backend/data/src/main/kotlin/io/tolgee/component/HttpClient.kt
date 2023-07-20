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
  fun <T> requestForJson(url: String, body: Any, method: HttpMethod, result: Class<T>): T? {
    val bodyJson = jacksonObjectMapper().writeValueAsString(body)
    val headers = HttpHeaders().apply {
      contentType = MediaType.APPLICATION_JSON
    }

    val response = restTemplate.exchange(
      url,
      method,
      HttpEntity(bodyJson, headers),
      String::class.java
    )

    @Suppress("UNNECESSARY_SAFE_CALL")
    return response?.body?.let { stringResponseBody ->
      jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(stringResponseBody, result)
    }
  }
}
