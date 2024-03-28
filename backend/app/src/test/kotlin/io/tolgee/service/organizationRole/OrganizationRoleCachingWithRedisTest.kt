/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service.organizationRole

import io.tolgee.fixtures.RedisRunner
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.AfterAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "spring.redis.port=56379",
  ],
)
@ContextConfiguration(initializers = [OrganizationRoleCachingWithRedisTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
class OrganizationRoleCachingWithRedisTest : OrganizationRoleCachingTest() {
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
      }
    }
  }
}
