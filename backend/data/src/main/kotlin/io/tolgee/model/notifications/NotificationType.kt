package io.tolgee.model.notifications

import io.tolgee.model.notifications.NotificationTypeGroup.ACCOUNT_SECURITY
import io.tolgee.model.notifications.NotificationTypeGroup.TASKS

enum class NotificationType(val group: NotificationTypeGroup) {
  TASK_ASSIGNED(TASKS),
  TASK_COMPLETED(TASKS),
  TASK_CLOSED(TASKS),
  MFA_ENABLED(ACCOUNT_SECURITY),
  MFA_DISABLED(ACCOUNT_SECURITY),
  PASSWORD_CHANGED(ACCOUNT_SECURITY),
}
