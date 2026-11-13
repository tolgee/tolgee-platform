package io.tolgee.dtos.response

data class NotificationKeySampleDto(
  val keyId: Long,
  val name: String,
  val namespace: String?,
  val baseText: String?,
)
