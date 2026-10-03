package io.tolgee.model.notifications

import io.tolgee.model.notifications.NotificationTypeGroup.ACCOUNT_SECURITY
import io.tolgee.model.notifications.NotificationTypeGroup.LOCALIZATION
import io.tolgee.model.notifications.NotificationTypeGroup.TASKS

enum class NotificationType(
  val group: NotificationTypeGroup,
  val grouped: Boolean = false,
  val countable: Boolean = false,
  val defaultInApp: Boolean = true,
  val defaultEmail: Boolean = true,
) {
  TASK_ASSIGNED(TASKS),
  TASK_FINISHED(TASKS),
  TASK_CANCELED(TASKS),
  MFA_ENABLED(ACCOUNT_SECURITY),
  MFA_DISABLED(ACCOUNT_SECURITY),
  PASSWORD_CHANGED(ACCOUNT_SECURITY),
  KEYS_ADDED(LOCALIZATION, grouped = true, countable = true),
  SOURCE_CHANGED(LOCALIZATION, grouped = true, countable = true),
  STRINGS_TRANSLATED(LOCALIZATION, grouped = true, countable = true),
  STRINGS_REVIEWED(LOCALIZATION, grouped = true, countable = true),
  AUTOMATICALLY_TRANSLATED(LOCALIZATION, grouped = true, defaultEmail = false),
  BULK_CHANGED(LOCALIZATION, grouped = true, defaultEmail = false),
  ;

  companion object {
    val GROUPED = entries.filter { it.grouped }
  }
}
