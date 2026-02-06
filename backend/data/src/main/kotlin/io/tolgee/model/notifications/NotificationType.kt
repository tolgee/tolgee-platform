package io.tolgee.model.notifications

import io.tolgee.model.notifications.NotificationTypeGroup.ACCOUNT_SECURITY
import io.tolgee.model.notifications.NotificationTypeGroup.BATCH_JOBS
import io.tolgee.model.notifications.NotificationTypeGroup.TASKS

enum class NotificationType(
  val group: NotificationTypeGroup,
) {
  TASK_ASSIGNED(TASKS),
  TASK_FINISHED(TASKS),
  TASK_CANCELED(TASKS),
  MFA_ENABLED(ACCOUNT_SECURITY),
  MFA_DISABLED(ACCOUNT_SECURITY),
  PASSWORD_CHANGED(ACCOUNT_SECURITY),
  BATCH_JOB_FINISHED(BATCH_JOBS),
}
