package io.tolgee.config

import com.posthog.server.PostHog
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestPostHogConfiguration {
  @Bean
  @Primary
  fun testPostHog(): PostHog {
    return Mockito.mock(PostHog::class.java)
  }
}
