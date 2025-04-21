/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.fixtures.RedisTest
import io.tolgee.fixtures.RedisTesting
import org.junit.jupiter.api.AfterAll
import org.springframework.test.context.ContextConfiguration

@RedisTest
@ContextConfiguration(initializers = [RedisTesting.Initializer::class])
class ScheduledUsageReportingRedisTest : ScheduledUsageReportingTest() {
  companion object {
    @AfterAll
    @JvmStatic
    fun stopRedis() {
      RedisTesting.stopRedis()
    }
  }
}
