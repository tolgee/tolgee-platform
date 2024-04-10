package io.tolgee.dtos.request.slack

import com.fasterxml.jackson.annotation.JsonProperty

data class SlackCommandDto(
  var token: String?,
  @JsonProperty("team_id")
  var teamId: String = "",
  @JsonProperty("enterprise_id")
  var channelId: String = "",
  var command: String = "",
  @JsonProperty("channel_id")
  var channelName: String = "",
  @JsonProperty("user_id")
  var userId: String = "",
  @JsonProperty("user_name")
  var userName: String = "",
  var text: String = "",
  @JsonProperty("trigger_id")
  var triggerId: String? = "",
  @JsonProperty("team_domain")
  var teamDomain: String = "",
)
