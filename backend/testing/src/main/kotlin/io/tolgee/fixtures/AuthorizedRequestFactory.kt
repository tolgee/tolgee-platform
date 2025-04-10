package io.tolgee.fixtures

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

object AuthorizedRequestFactory {
  var token: String? = null

  @JvmStatic
  fun init(token: String?) {
    AuthorizedRequestFactory.token = token
  }

  @JvmStatic
  fun loggedGet(url: String): MockHttpServletRequestBuilder = addToken(MockMvcRequestBuilders.get(url))

  @JvmStatic
  fun loggedPost(url: String): MockHttpServletRequestBuilder = addToken(MockMvcRequestBuilders.post(url))

  @JvmStatic
  fun loggedPut(url: String): MockHttpServletRequestBuilder = addToken(MockMvcRequestBuilders.put(url))

  @JvmStatic
  fun loggedDelete(url: String): MockHttpServletRequestBuilder = addToken(MockMvcRequestBuilders.delete(url))

  fun addToken(builder: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
    builder.header("Authorization", getBearerTokenString(token))

  fun getBearerTokenString(token: String?) = String.format("Bearer %s", token)
}
