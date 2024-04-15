package io.tolgee.configuration

import com.slack.api.Slack
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SlackConfiguration {
  @Bean
  fun slackClient(): Slack {
    val instance = Slack.getInstance()
    println("Creating Slack instance: $instance")
    return instance
  }
}
