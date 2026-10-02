package io.tolgee.dtos.response

data class NotificationLanguageDto(
  val id: Long,
  val tag: String,
  val name: String,
  val flagEmoji: String?,
)
