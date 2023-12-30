package io.tolgee.security.rateLimits

import io.tolgee.constants.Caches
import io.tolgee.fixtures.RedisRunner
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsRateLimited
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration

@AutoConfigureMockMvc
@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "tolgee.cache.use-redis=true",
    "spring.redis.port=56379",
    "tolgee.rate-limits.ip-request-limit=2",
    "tolgee.rate-limits.ip-request-window=10000",
  ],
)
@ContextConfiguration(initializers = [RedisRateLimitsTest.Companion.Initializer::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisRateLimitsTest : AuthorizedControllerTest() {
  companion object {
    val redisRunner = RedisRunner()

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
      }
    }

    @AfterAll
    @JvmStatic
    fun cleanup() {
      redisRunner.stop()
    }
  }

  @BeforeEach
  fun clearCache() {
    cacheManager.getCache(Caches.RATE_LIMITS)?.clear()
  }

  @Test
  fun `ip request limit works`() {
    performGet("/api/public/configuration").andIsOk
    performGet("/api/public/configuration").andIsOk
    performGet("/api/public/configuration").andIsRateLimited
  }
}
