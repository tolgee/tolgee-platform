package io.tolgee.development.testDataBuilder.builders.slack

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.slackIntegration.SavedSlackMessage

class SavedSlackMessageBuilder(
  slackConfigBuilder: SlackConfigBuilder,
) : EntityDataBuilder<SavedSlackMessage, SavedSlackMessageBuilder> {
  override var self = SavedSlackMessage("", slackConfigBuilder.self, 0, setOf(), false)
}
