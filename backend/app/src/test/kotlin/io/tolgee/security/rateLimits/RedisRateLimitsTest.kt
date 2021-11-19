package io.tolgee.security.rateLimits

import io.tolgee.fixtures.RedisRunner
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testng.annotations.AfterClass
import org.testng.annotations.Test

@AutoConfigureMockMvc
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "tolgee.cache.use-redis=true",
    "spring.redis.port=56379",
  ]
)
@ContextConfiguration(initializers = [RedisRateLimitsTest.Companion.Initializer::class])
class RedisRateLimitsTest : AbstractRateLimitsTest() {
  companion object {
    val redisRunner = RedisRunner()

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
      }
    }
  }

  @AfterClass(alwaysRun = true)
  fun cleanup() {
    redisRunner.stop()
  }

  @Test
  override fun `it doesn't allow more then set in configuration`() {
    super.`it doesn't allow more then set in configuration`()
  }
}
