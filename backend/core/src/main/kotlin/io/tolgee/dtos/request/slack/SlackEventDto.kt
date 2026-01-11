package io.tolgee.dtos.request.slack

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackEventDto(
  val token: String,
  val user: User,
  val type: String,
  val channel: Channel,
  val actions: List<ActionDetail>,
  @JsonProperty("trigger_id")
  val triggerId: String,
  val team: Team,
) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class Channel(
    val id: String,
  )

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class User(
    val id: String,
  )

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class ActionDetail(
    @JsonProperty("action_id") val actionId: String,
    @JsonProperty("block_id") val blockId: String,
    val value: String?,
    val type: String,
  )

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class Team(
    val id: String,
    val domain: String,
  )
}
