package io.tolgee.component.bucket

import io.tolgee.fixtures.RedisRunner
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.AfterAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.websocket.use-redis=true",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ContextConfiguration(initializers = [TokenBucketManagerTestWithRedis.Companion.Initializer::class])
@ContextRecreatingTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TokenBucketManagerTestWithRedis : AbstractTokenBucketManagerTest() {
  companion object {
    val redisRunner = RedisRunner()

    @AfterAll
    @JvmStatic
    fun stopRedis() {
      redisRunner.stop()
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
        TestPropertyValues
          .of("spring.data.redis.port=${RedisRunner.port}")
          .applyTo(configurableApplicationContext)
      }
    }
  }
}
