package io.tolgee.dtos.request

data class SlackCommandDto(
  var token: String?,
  var channel_id: String = "",
  var channel_name: String? = "",
  var userId: String = "",
  var userName: String?,
  var text: String = ""
) {
}
