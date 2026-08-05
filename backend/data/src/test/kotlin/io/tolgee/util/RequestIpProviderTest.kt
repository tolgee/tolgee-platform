package io.tolgee.util

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.SessionAuditProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class RequestIpProviderTest {
  @AfterEach
  fun cleanup() {
    RequestContextHolder.resetRequestAttributes()
  }

  @Test
  fun `without a proxy it reports the address the connection came from`() {
    request(remoteAddr = "203.0.113.7", forwardedFor = "8.8.8.8")

    provider(trustedProxyCount = 0).getTrustedClientIp().assert.isEqualTo("203.0.113.7")
  }

  /**
   * The whole point of counting from the end: a client that prepends its own entry cannot push
   * itself into the position the proxy writes.
   */
  @Test
  fun `behind one proxy a forged entry cannot displace the observed address`() {
    request(remoteAddr = "10.0.0.1", forwardedFor = "8.8.8.8, 203.0.113.7")

    provider(trustedProxyCount = 1).getTrustedClientIp().assert.isEqualTo("203.0.113.7")
  }

  @Test
  fun `a header shorter than the configured hops falls back to the connection`() {
    request(remoteAddr = "10.0.0.1", forwardedFor = "203.0.113.7")

    provider(trustedProxyCount = 2).getTrustedClientIp().assert.isEqualTo("10.0.0.1")
  }

  @Test
  fun `a missing header falls back to the connection`() {
    request(remoteAddr = "10.0.0.1", forwardedFor = null)

    provider(trustedProxyCount = 1).getTrustedClientIp().assert.isEqualTo("10.0.0.1")
  }

  private fun request(
    remoteAddr: String,
    forwardedFor: String?,
  ) {
    val request = MockHttpServletRequest()
    request.remoteAddr = remoteAddr
    forwardedFor?.let { request.addHeader("X-Forwarded-For", it) }
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
  }

  private fun provider(trustedProxyCount: Int): RequestIpProvider {
    val properties =
      TolgeeProperties().apply {
        authentication =
          AuthenticationProperties().apply {
            sessionAudit = SessionAuditProperties().apply { this.trustedProxyCount = trustedProxyCount }
          }
      }
    return RequestIpProvider(properties)
  }
}
