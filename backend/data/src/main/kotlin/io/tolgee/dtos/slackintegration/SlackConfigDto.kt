package io.tolgee.dtos.slackintegration

import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.slackIntegration.SlackEventType

data class SlackConfigDto(
  val project: Project,
  val slackId: String = "",
  val channelId: String,
  val userAccount: UserAccount,
  val languageTag: String? = "",
  val events: MutableSet<SlackEventType> = mutableSetOf(),
  val slackTeamId: String = "",
  val isGlobal: Boolean? = false,
)
