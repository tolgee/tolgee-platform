package io.tolgee.dtos.request.slack

data class SlackConnectionDto(
  val slackId: String,
  val channelId: String,
  val workspaceId: Long,
)
