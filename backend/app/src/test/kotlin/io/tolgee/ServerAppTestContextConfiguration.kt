package io.tolgee

import io.tolgee.testing.TestOverridesConfiguration
import io.tolgee.testing.TestsConfiguration
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(
  classes = [
    Application::class,
    TestsConfiguration::class,
    TestOverridesConfiguration::class,
    ServerAppTestOverridesConfiguration::class
  ]
)
annotation class ServerAppTestContextConfiguration()
