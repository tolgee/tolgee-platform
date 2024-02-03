package io.tolgee.dtos.request

data class SlackEventDto(
  val type: String,
  val team: Team,
  val user: User,
  val api_app_id: String,
  val token: String,
  val container: Container,
  val trigger_id: String,
  val channel: Channel,
  val message: Message,
  val response_url: String,
  val actions: List<Action>
)

data class Team(
  val id: String,
  val domain: String
)

data class User(
  val id: String,
  val username: String,
  val team_id: String
)

data class Container(
  val type: String,
  val message_ts: String,
  val attachment_id: Int,
  val channel_id: String,
  val is_ephemeral: Boolean,
  val is_app_unfurl: Boolean
)

data class Channel(
  val id: String,
  val name: String
)

data class Message(
  val bot_id: String,
  val type: String,
  val text: String,
  val user: String,
  val ts: String
  // Add other fields as needed
)

data class Action(
  val action_id: String,
  val block_id: String,
  val text: Text,
  val value: String,
  val type: String,
  val action_ts: String
)

data class Text(
  val type: String,
  val text: String,
  val emoji: Boolean
)
