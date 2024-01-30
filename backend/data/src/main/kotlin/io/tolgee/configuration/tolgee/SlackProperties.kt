package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.slack")
class SlackProperties {
  var slackToken: String = "xoxb-6460981223175-6480877302916-kVJzxYw4v4AdNjX4x3wWck32"
}
