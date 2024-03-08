package io.tolgee.dtos.slackintegration

import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.slackIntegration.EventName

data class SlackConfigDto (
  val project: Project,
  val slackId: String = "",
  val channelId: String,
  val userAccount: UserAccount,
  val languageTag: String? = "",
  val onEvent: EventName?
)
