package io.tolgee.unit

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class TolgeePropertiesValidationTest {
  @EnableConfigurationProperties(TolgeeProperties::class)
  class Config

  private val contextRunner = ApplicationContextRunner().withUserConfiguration(Config::class.java)

  @Test
  fun `context starts with valid configuration`() {
    contextRunner.run { context -> assertThat(context).hasNotFailed() }
  }

  @Test
  fun `rejects invalid webhook configuration at startup`() {
    contextRunner
      .withPropertyValues(
        "tolgee.webhook.auto-disable-warning-after-hours=100",
        "tolgee.webhook.auto-disable-after-days=1",
      ).run { context -> assertThat(context).hasFailed() }
  }

  @Test
  fun `rejects invalid deeply-nested sso-global configuration at startup`() {
    contextRunner
      .withPropertyValues("tolgee.authentication.sso-global.enabled=true")
      .run { context -> assertThat(context).hasFailed() }
  }
}
