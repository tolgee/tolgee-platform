package io.tolgee.api.v2.controllers.apps

import io.tolgee.fixtures.RedisRunner
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.AfterAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.internal.fake-mt-providers=false",
  ],
)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = [AppCacheWithRedisTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AppCacheWithRedisTest : AbstractAppCacheTest() {
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
