package io.tolgee.development.testDataBuilder.builders.slack

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.slackIntegration.SlackConfig

class SlackConfigBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<SlackConfig, SlackConfigBuilder> {
  val userAccountBuilder = UserAccountBuilder(projectBuilder.testDataBuilder)
  override var self = SlackConfig(projectBuilder.self, userAccountBuilder.self, "")
}
