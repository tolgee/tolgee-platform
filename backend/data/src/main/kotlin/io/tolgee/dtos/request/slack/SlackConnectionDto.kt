package io.tolgee.dtos.request.slack

data class SlackConnectionDto(
  val slackId: String = "",
  val userAccountId: String = "",
  val channelId: String = "",
  val slackNickName: String = "",
  val workspace: String = "",
  val orgId: String = "",
  val channelName: String = "",
  val author: String,
  val workSpaceName: String,
)
