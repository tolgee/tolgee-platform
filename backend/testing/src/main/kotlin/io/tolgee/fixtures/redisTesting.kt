package io.tolgee.fixtures

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

object RedisTesting {
  val redisRunner = RedisRunner()

  fun stopRedis() {
    redisRunner.stop()
  }

  class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
      redisRunner.run()
    }
  }
}

@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "spring.data.redis.port=56379",
  ],
)
annotation class RedisTest
