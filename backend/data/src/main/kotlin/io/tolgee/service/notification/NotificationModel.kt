package io.tolgee.service.notification

data class NotificationModel(
  val id: Long,
  val projectId: Long?,
  val linkedTaskNumber: Long?,
  val linkedTaskName: String?,
)
