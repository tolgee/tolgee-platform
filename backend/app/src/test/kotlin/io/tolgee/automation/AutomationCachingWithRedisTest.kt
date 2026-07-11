/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.automation

import io.tolgee.fixtures.RedisTest
import io.tolgee.fixtures.RedisTesting
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.AfterAll
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

@RedisTest
@ContextConfiguration(initializers = [RedisTesting.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
class AutomationCachingWithRedisTest : AutomationCachingTest() {
  companion object {
    @AfterAll
    @JvmStatic
    fun stopRedis() {
      RedisTesting.stopRedis()
    }
  }
}
