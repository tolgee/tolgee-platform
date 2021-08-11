package io.tolgee.cache

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.fixtures.DockerContainerRunner
import org.redisson.spring.cache.RedissonSpringCacheManager
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testng.annotations.Test

@SpringBootTest(
  properties = [
    "spring.redis.port=56379",
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
  ]
)
@ContextConfiguration(initializers = [CacheWithRedisTest.Companion.Initializer::class])
class CacheWithRedisTest : AbstractCacheTest() {
  companion object {
    val redisRunner = DockerContainerRunner(
      image = "redis:6",
      expose = mapOf("56379" to "6379"),
      name = "server-integration-test-redis",
      waitForLog = "Ready to accept connections"
    )

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
      }
    }
  }

  @Test
  fun `it has proper cache manager`() {
    assertThat(cacheManager).isInstanceOf(RedissonSpringCacheManager::class.java)
  }
}
