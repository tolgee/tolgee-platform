package io.tolgee.ee.service.eeSubscription

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.exceptions.ErrorResponseBody
import org.springframework.web.client.RestClientResponseException
import java.io.Serializable

private val objectMapper =
  jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

/**
 * Only `code` is load-bearing for the callers, so it is read on its own: a `params` array we cannot
 * hold must not cost us the code. readTree("") yields MissingNode rather than throwing, which the
 * isTextual check turns into null.
 */
fun RestClientResponseException.parseTolgeeErrorBody(): ErrorResponseBody? {
  val root =
    try {
      objectMapper.readTree(this.responseBodyAsString)
    } catch (e: JacksonException) {
      return null
    }
  val code = root.path("code").takeIf { it.isTextual }?.asText() ?: return null
  val params =
    root.path("params").takeIf { it.isArray }?.let {
      try {
        objectMapper.convertValue(it, object : TypeReference<List<Serializable?>>() {})
      } catch (e: IllegalArgumentException) {
        null
      }
    }
  return ErrorResponseBody(code, params)
}
