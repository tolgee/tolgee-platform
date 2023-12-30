package io.tolgee.fixtures

import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

interface RequestPerformer {
  fun perform(builder: MockHttpServletRequestBuilder): ResultActions

  fun performPut(
    url: String,
    content: Any?,
    httpHeaders: HttpHeaders = HttpHeaders.EMPTY,
  ): ResultActions

  fun performPost(
    url: String,
    content: Any?,
    httpHeaders: HttpHeaders = HttpHeaders.EMPTY,
  ): ResultActions

  fun performGet(
    url: String,
    httpHeaders: HttpHeaders = HttpHeaders.EMPTY,
  ): ResultActions

  fun performDelete(
    url: String,
    content: Any?,
    httpHeaders: HttpHeaders = HttpHeaders.EMPTY,
  ): ResultActions
}
