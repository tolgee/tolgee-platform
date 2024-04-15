package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.slackIntegration.SlackConfig

class SlackConfigBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<SlackConfig, SlackConfigBuilder> {
  val userAccountBuilder = UserAccountBuilder(projectBuilder.testDataBuilder)
  override var self = SlackConfig(projectBuilder.self, userAccountBuilder.self, "")
}
