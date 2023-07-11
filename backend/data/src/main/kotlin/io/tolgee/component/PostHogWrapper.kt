package io.tolgee.component

import com.posthog.java.PostHog
import io.tolgee.configuration.tolgee.PostHogProperties
import org.springframework.stereotype.Component
import javax.annotation.PreDestroy

@Component
class PostHogWrapper(
  private val properties: PostHogProperties
) {

  val postHog by lazy {
    properties.apiKey?.let { postHogApiKey ->
      PostHog.Builder(postHogApiKey).also { builder -> properties.host?.let { builder.host(it) } }.build()
    }
  }

  @PreDestroy
  fun shutdown() {
    postHog?.shutdown()
  }
}
