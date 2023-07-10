package io.tolgee.configuration

import com.posthog.java.PostHog
import io.tolgee.configuration.tolgee.PostHogProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.annotation.PreDestroy

@Configuration
class PostHogConfiguration(
  private val properties: PostHogProperties
) {

  @Bean
  fun postHog(): PostHog? {
    return properties.apiKey?.let { postHogApiKey ->
      PostHog.Builder(postHogApiKey).also { builder -> properties.host?.let { builder.host(it) } }.build()
    }
  }

  @PreDestroy
  fun destroy() {
    postHog()?.shutdown()
  }
}
