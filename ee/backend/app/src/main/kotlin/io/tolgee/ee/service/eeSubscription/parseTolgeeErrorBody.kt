package io.tolgee.ee.service.eeSubscription

import io.tolgee.exceptions.ErrorResponseBody
import org.springframework.web.client.RestClientResponseException
import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue

private val objectMapper =
  jacksonMapperBuilder()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .build()

fun RestClientResponseException.parseTolgeeErrorBody(): ErrorResponseBody? {
  return try {
    objectMapper.readValue<ErrorResponseBody>(this.responseBodyAsString)
  } catch (e: JacksonException) {
    null
  }
}
