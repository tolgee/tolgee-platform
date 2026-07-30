package io.tolgee.ee.service.eeSubscription

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.exceptions.ErrorResponseBody
import org.springframework.web.client.RestClientResponseException

private val objectMapper =
  jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

fun RestClientResponseException.parseTolgeeErrorBody(): ErrorResponseBody? {
  return try {
    objectMapper.readValue(this.responseBodyAsString, ErrorResponseBody::class.java)
  } catch (e: JacksonException) {
    null
  }
}
