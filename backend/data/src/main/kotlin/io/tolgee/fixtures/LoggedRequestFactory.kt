package io.tolgee.fixtures

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

object LoggedRequestFactory {
  var token: String? = null

  @JvmStatic
  fun init(token: String?) {
    LoggedRequestFactory.token = token
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
    return builder.header("Authorization", String.format("Bearer %s", token))
  }
}
