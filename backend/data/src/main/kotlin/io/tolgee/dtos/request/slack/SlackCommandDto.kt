package io.tolgee.dtos.request.slack

data class SlackCommandDto(
  var token: String?,
  var team_id: String = "",
  var channel_id: String = "",
  var command: String = "",
  var channel_name: String = "",
  var user_id: String = "",
  var user_name: String = "",
  var text: String = "",
  var trigger_id: String? = "",
  var team_domain: String = "",
)
