package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.slack")
class SlackProperties {
  var slackToken: String = "xoxb-6460981223175-6480877302916-h7QfJvg7cRQXzrIhTwo1mAgt"
}
