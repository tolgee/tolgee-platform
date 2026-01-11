package io.tolgee.configuration

import com.slack.api.Slack
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SlackConfiguration {
  @Bean
  fun slackClint(): Slack {
    return Slack.getInstance()
  }
}
