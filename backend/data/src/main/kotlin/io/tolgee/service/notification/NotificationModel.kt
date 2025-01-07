package io.tolgee.service.notification

data class NotificationModel(
  val id: Long,
  val linkedProjectId: Long?,
  val linkedTaskNumber: Long?,
  val linkedTaskName: String?,
)
