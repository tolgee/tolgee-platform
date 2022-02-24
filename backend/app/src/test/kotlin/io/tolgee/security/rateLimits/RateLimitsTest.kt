package io.tolgee.security.rateLimits

import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.Test
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
  @Test
  fun `ip request limit works`() {
    testEndpoint("ip") { performGet("/api/public/configuration") }
  }

  @Test
  fun `user request limit works`() {
    testEndpoint("user_id") { performAuthGet("/v2/projects") }
  }

  @Test
  fun `limits auth endpoints`() {
    testEndpoint("auth_req_ip", expectedStatus = 401) {
      performPost("/api/public/generatetoken?bla", mapOf("username" to "a", "password" to "p"))
    }
    testEndpoint("auth_req_ip") {
      performPost("/api/public/reset_password_request?bla", mapOf("callbackUrl" to "a", "email" to "a@a.az"))
    }
  }
}
