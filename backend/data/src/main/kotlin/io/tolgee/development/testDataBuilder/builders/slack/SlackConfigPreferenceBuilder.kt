package io.tolgee.development.testDataBuilder.builders.slack

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.slackIntegration.SlackConfigPreference

class SlackConfigPreferenceBuilder(
  slackConfigBuilder: SlackConfigBuilder,
) : EntityDataBuilder<SlackConfigPreference, SlackConfigPreferenceBuilder> {
  override var self = SlackConfigPreference(slackConfigBuilder.self)
}
