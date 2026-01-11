package io.tolgee.configuration

import com.posthog.server.PostHog
import com.posthog.server.PostHogConfig
import io.tolgee.configuration.tolgee.PostHogProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PostHogConfiguration(
  private val properties: PostHogProperties,
) {
  @Bean(destroyMethod = "close")
  fun postHog(): PostHog? {
    return properties.apiKey?.let { postHogApiKey ->
      val configBuilder = PostHogConfig.builder(postHogApiKey)
      properties.host?.let { configBuilder.host(it) }
      val config = configBuilder.build()

      return getPostHogInstance(config)
    }
  }

  /**
   * This is not official way how to create the PostHog instance.
   *
   * But when I used the official way, it was returning PostHogInterface, and it was impossible for us to mock it.
   */
  private fun getPostHogInstance(config: PostHogConfig): PostHog {
    val instance = PostHog()
    instance.setup(config)
    return instance
  }
}
