package io.tolgee.security.rateLimits

import io.tolgee.fixtures.RedisRunner
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.AfterAll
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
  ]
)
@ContextConfiguration(initializers = [RedisRateLimitsTest.Companion.Initializer::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisRateLimitsTest : AbstractRateLimitsTest() {
  companion object {
    val redisRunner = RedisRunner()

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
      }
    }
  }

  @AfterAll
  fun cleanup() {
    redisRunner.stop()
  }

  @Test
  fun `ip request limit works`() {
    testRateLimit("ip") { performGet("/api/public/configuration") }
  }
}
