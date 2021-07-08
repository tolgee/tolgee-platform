package io.tolgee.fixtures

import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

interface RequestPerformer {
  fun perform(builder: MockHttpServletRequestBuilder): ResultActions
  fun performPut(url: String, content: Any?): ResultActions
  fun performPost(url: String, content: Any?): ResultActions
  fun performGet(url: String): ResultActions
  fun performDelete(url: String, content: Any?): ResultActions
}
