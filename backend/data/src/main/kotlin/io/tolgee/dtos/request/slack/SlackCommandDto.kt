package io.tolgee.dtos.request.slack

data class SlackCommandDto(
  val token: String?,
  val team_id: String,
  val channel_id: String,
  val command: String,
  val channel_name: String,
  val user_id: String,
  var user_name: String,
  val text: String,
  val trigger_id: String?,
  val team_domain: String,
)
