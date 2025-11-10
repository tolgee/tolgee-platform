package io.tolgee.fixtures

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class NdJsonParser(
  objectMapper: ObjectMapper,
) {
  fun parse(string: String): List<Any?> {
    return string.split("\n").filter { it.isNotBlank() }.map {
      jacksonObjectMapper().readValue(it, Any::class.java)
    }
  }
}
