package io.tolgee.configuration

import com.posthog.server.PostHog
import com.posthog.server.PostHogConfig
import com.posthog.server.PostHogInterface
import io.tolgee.configuration.tolgee.PostHogProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PostHogConfiguration(
  private val properties: PostHogProperties,
) {
  @Bean(destroyMethod = "close")
  fun postHog(): PostHogInterface? {
    return properties.apiKey?.let { postHogApiKey ->
      val configBuilder = PostHogConfig.builder(postHogApiKey)
      properties.host?.let { configBuilder.host(it) }
      val config = configBuilder.build()
      PostHog.with(config)
    }
  }
}
