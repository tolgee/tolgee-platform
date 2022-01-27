package io.tolgee.cache

import io.tolgee.fixtures.RedisRunner
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.redisson.spring.cache.RedissonSpringCacheManager
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "spring.redis.port=56379",
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.internal.fake-mt-providers=false",
    "tolgee.machine-translation.free-credits-amount=10000000"
  ]
)
@ContextConfiguration(initializers = [CacheWithRedisTest.Companion.Initializer::class])
class CacheWithRedisTest : AbstractCacheTest() {
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
  fun `it has proper cache manager`() {
    assertThat(cacheManager).isInstanceOf(RedissonSpringCacheManager::class.java)
  }
}
