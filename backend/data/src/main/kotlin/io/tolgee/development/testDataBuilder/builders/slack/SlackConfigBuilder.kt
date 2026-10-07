package io.tolgee.development.testDataBuilder.builders.slack

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.development.testDataBuilder.builders.BaseEntityDataBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackConfigPreference

class SlackConfigBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<SlackConfig, SlackConfigBuilder>() {
  class DATA {
    var slackMessages = mutableListOf<SavedSlackMessageBuilder>()
    var preferences = mutableListOf<SlackConfigPreferenceBuilder>()
  }

  var data = DATA()

  fun addSlackMessage(ft: FT<SavedSlackMessage>) = addOperation(data.slackMessages, ft)

  fun addPreference(ft: FT<SlackConfigPreference>) =
    addOperation(data.preferences, ft).also { self.preferences.add(it.self) }

  val userAccountBuilder = UserAccountBuilder(projectBuilder.testDataBuilder)
  override var self = SlackConfig(projectBuilder.self, userAccountBuilder.self, "")
}
