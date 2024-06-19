package io.tolgee.development.testDataBuilder.builders.slack

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.slackIntegration.SlackUserConnection

class SlackUserConnectionBuilder(
  val userAccountBuilder: UserAccountBuilder,
) : EntityDataBuilder<SlackUserConnection, SlackUserConnectionBuilder> {
  override var self = SlackUserConnection()
}
