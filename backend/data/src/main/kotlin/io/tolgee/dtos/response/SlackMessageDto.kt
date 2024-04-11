package io.tolgee.dtos.response

data class SlackMessageDto(
  val text: String? = null,
  val blocks: List<Any>? = null,
)
