package io.tolgee.ee.service.eeSubscription

import io.tolgee.exceptions.ErrorResponseBody
import org.springframework.web.client.RestClientResponseException
import tools.jackson.core.JacksonException
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

private val objectMapper = jacksonObjectMapper()

fun RestClientResponseException.parseTolgeeErrorBody(): ErrorResponseBody? {
  return try {
    objectMapper.readValue<ErrorResponseBody>(this.responseBodyAsString)
  } catch (e: JacksonException) {
    null
  }
}
