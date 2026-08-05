package io.tolgee.fixtures

import org.springframework.test.web.servlet.MvcResult
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

inline fun <reified T> MvcResult.mapResponseTo(): T {
  return jacksonObjectMapper().readValue(this.response.contentAsString)
}
