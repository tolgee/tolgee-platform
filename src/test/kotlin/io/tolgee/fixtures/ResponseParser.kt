package io.tolgee.fixtures

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.test.web.servlet.MvcResult

inline fun <reified T> MvcResult.mapResponseTo(): T {
  return jacksonObjectMapper().readValue(this.response.contentAsString)
}
