package io.tolgee.dtos.request.slack

data class SlackConnectionDto(
  val slackId: String = "",
  val userAccountId: String = "",
  val channelId: String = "",
  val slackNickName: String = "",
)
