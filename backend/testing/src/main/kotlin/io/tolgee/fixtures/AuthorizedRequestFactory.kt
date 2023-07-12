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
  fun loggedGet(url: String): MockHttpServletRequestBuilder {
    return addToken(MockMvcRequestBuilders.get(url))
  }

  @JvmStatic
  fun loggedPost(url: String): MockHttpServletRequestBuilder {
    return addToken(MockMvcRequestBuilders.post(url))
  }

  @JvmStatic
  fun loggedPut(url: String): MockHttpServletRequestBuilder {
    return addToken(MockMvcRequestBuilders.put(url))
  }

  @JvmStatic
  fun loggedDelete(url: String): MockHttpServletRequestBuilder {
    return addToken(MockMvcRequestBuilders.delete(url))
  }

  fun addToken(builder: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder {
    return builder.header("Authorization", getBearerTokenString(token))
  }

  fun getBearerTokenString(token: String?) = String.format("Bearer %s", token)
}
