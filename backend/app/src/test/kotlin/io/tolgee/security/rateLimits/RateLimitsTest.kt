package io.tolgee.security.rateLimits

import io.tolgee.AbstractServerAppAuthorizedControllerTest
import io.tolgee.constants.Caches
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsRateLimited
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@AutoConfigureMockMvc
@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "tolgee.rate-limits.ip-request-limit=10",
    "tolgee.rate-limits.ip-request-window=10000",
    "tolgee.rate-limits.user-request-limit=15",
    "tolgee.rate-limits.user-request-window=10000",
  ]
)
class RateLimitsTest : AbstractServerAppAuthorizedControllerTest() {
  @BeforeEach
  fun clearCache() {
    cacheManager.getCache(Caches.RATE_LIMITS)?.clear()
  }

  @Test
  fun `ip request limit works`() {
    (1..10).forEach { _ ->
      performGet("/api/public/configuration").andIsOk
    }
    performGet("/api/public/configuration").andIsRateLimited
  }

  @Test
  fun `user request limit works`() {
    (1..15).forEach { _ ->
      performAuthGet("/v2/projects").andIsOk
    }
    performAuthGet("/v2/projects").andIsRateLimited
  }

  @Test
  fun `limits auth endpoints`() {
    (1..5).forEach { _ ->
      performPost("/api/public/generatetoken?bla", mapOf("username" to "a", "password" to "p")).andIsUnauthorized
    }
    performPost("/api/public/generatetoken?bla", mapOf("username" to "a", "password" to "p")).andIsRateLimited
  }
}
