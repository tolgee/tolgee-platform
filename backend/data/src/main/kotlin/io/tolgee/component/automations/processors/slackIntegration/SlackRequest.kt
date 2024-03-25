package io.tolgee.component.automations.processors.slackIntegration

import io.tolgee.api.IProjectActivityModel

data class SlackRequest(
  val activityData: IProjectActivityModel?,
)
