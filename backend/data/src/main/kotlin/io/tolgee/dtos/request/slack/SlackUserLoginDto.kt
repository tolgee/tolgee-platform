package io.tolgee.dtos.request.slack

data class SlackUserLoginDto(
  val slackUserId: String,
  val slackChannelId: String,
  val workspaceId: Long?,
  val slackTeamId: String,
)
