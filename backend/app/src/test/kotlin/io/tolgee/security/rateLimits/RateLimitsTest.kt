package io.tolgee.security.rateLimits

import io.tolgee.configuration.tolgee.RateLimitProperties
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@AutoConfigureMockMvc
@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ]
)
class RateLimitsTest : AbstractRateLimitsTest() {

  @Autowired
  lateinit var rateLimitProperties: RateLimitProperties

  @Test
  fun `ip request limit works`() {
    testRateLimit("ip") { performGet("/api/public/configuration") }
  }

  @Test
  fun `user request limit works`() {
    testRateLimit("user_id") { performAuthGet("/v2/projects") }
  }

  @Test
  fun `limits auth endpoints`() {
    testRateLimit("auth_req_ip", expectedStatus = 401) {
      performPost("/api/public/generatetoken?bla", mapOf("username" to "a", "password" to "p"))
    }
    testRateLimit("auth_req_ip") {
      performPost("/api/public/reset_password_request?bla", mapOf("callbackUrl" to "a", "email" to "a@a.az"))
    }
  }

  @Test
  fun `can be disabled by property`() {
    rateLimitProperties.enabled = false
    testRateLimitDisabled("auth_req_ip", expectedStatus = 401) {
      performPost("/api/public/generatetoken?bla", mapOf("username" to "a", "password" to "p"))
    }
    rateLimitProperties.enabled = true
  }
}
