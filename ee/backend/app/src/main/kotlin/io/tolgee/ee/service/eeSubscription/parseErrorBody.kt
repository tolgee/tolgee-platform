package io.tolgee.ee.service.eeSubscription

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.exceptions.ErrorResponseBody
import org.springframework.web.client.RestClientResponseException

internal fun RestClientResponseException.parseErrorBody(): ErrorResponseBody? {
  return try {
    jacksonObjectMapper().readValue(this.responseBodyAsString, ErrorResponseBody::class.java)
  } catch (e: JacksonException) {
    null
  }
}
