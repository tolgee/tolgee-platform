package io.tolgee.component

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class HttpClient(
  private val restTemplate: RestTemplate,
) {
  /**
   * @param lenientEnums for a remote that may be newer than this instance — an unknown enum value
   * falls back instead of failing the whole response.
   */
  fun <T> requestForJson(
    url: String,
    body: Any,
    method: HttpMethod,
    result: Class<T>,
    headers: HttpHeaders = HttpHeaders(),
    lenientEnums: Boolean = false,
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

    val mapper = if (lenientEnums) LENIENT_ENUM_RESPONSE_MAPPER else RESPONSE_MAPPER
    return response.body.let { stringResponseBody ->
      mapper.readValue(stringResponseBody, result)
    }
  }

  companion object {
    private val RESPONSE_MAPPER: ObjectMapper =
      jacksonMapperBuilder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()

    val LENIENT_ENUM_RESPONSE_MAPPER: ObjectMapper =
      jacksonMapperBuilder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
        .build()
  }
}
