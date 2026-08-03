package io.tolgee.unit.component

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class FrontendUrlProviderTest {
  @Test
  fun `explains the missing configuration when there is no current request`() {
    RequestContextHolder.resetRequestAttributes()

    assertThatThrownBy { FrontendUrlProvider(TolgeeProperties()).url }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("specify frontend url in application properties")
  }

  @Test
  fun `derives the url from the current request`() {
    withCurrentRequest {
      assertThat(FrontendUrlProvider(TolgeeProperties()).url).isEqualTo("https://app.example.com")
    }
  }

  @Test
  fun `explains the missing configuration when the current attributes are not servlet ones`() {
    RequestContextHolder.setRequestAttributes(NonServletRequestAttributes())
    try {
      assertThatThrownBy { FrontendUrlProvider(TolgeeProperties()).url }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("specify frontend url in application properties")
    } finally {
      RequestContextHolder.resetRequestAttributes()
    }
  }

  private class NonServletRequestAttributes : RequestAttributes {
    override fun getAttribute(
      name: String,
      scope: Int,
    ): Any? = null

    override fun setAttribute(
      name: String,
      value: Any,
      scope: Int,
    ) = Unit

    override fun removeAttribute(
      name: String,
      scope: Int,
    ) = Unit

    override fun getAttributeNames(scope: Int): Array<String> = emptyArray()

    override fun registerDestructionCallback(
      name: String,
      callback: Runnable,
      scope: Int,
    ) = Unit

    override fun resolveReference(key: String): Any? = null

    override fun getSessionId(): String = "session"

    override fun getSessionMutex(): Any = this
  }

  private fun withCurrentRequest(fn: () -> Unit) {
    val request =
      MockHttpServletRequest().apply {
        scheme = "https"
        serverName = "app.example.com"
        serverPort = 443
        requestURI = "/v2/projects"
        queryString = "page=1"
      }
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    try {
      fn()
    } finally {
      RequestContextHolder.resetRequestAttributes()
    }
  }

  @Test
  fun `prefers the configured url over the current request`() {
    withCurrentRequest {
      val properties = TolgeeProperties().apply { frontEndUrl = "https://configured.example.com" }
      assertThat(FrontendUrlProvider(properties).url).isEqualTo("https://configured.example.com")
    }
  }
}
