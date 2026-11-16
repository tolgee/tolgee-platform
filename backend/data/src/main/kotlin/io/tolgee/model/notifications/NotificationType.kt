package io.tolgee.model.notifications

import io.tolgee.model.notifications.NotificationEmailTiming.MENTION_DELAYED
import io.tolgee.model.notifications.NotificationEmailTiming.WORK_GROUPED
import io.tolgee.model.notifications.NotificationEntityKind.KEY
import io.tolgee.model.notifications.NotificationEntityKind.TRANSLATION
import io.tolgee.model.notifications.NotificationTypeGroup.ACCOUNT_SECURITY
import io.tolgee.model.notifications.NotificationTypeGroup.COMMENTS
import io.tolgee.model.notifications.NotificationTypeGroup.LOCALIZATION
import io.tolgee.model.notifications.NotificationTypeGroup.TASKS

enum class NotificationType(
  val group: NotificationTypeGroup,
  val grouped: Boolean = false,
  val countable: Boolean = false,
  val defaultInApp: Boolean = true,
  val defaultEmail: Boolean = true,
  val emailTiming: NotificationEmailTiming = NotificationEmailTiming.IMMEDIATE,
  val entityKind: NotificationEntityKind? = null,
) {
  TASK_ASSIGNED(TASKS),
  TASK_FINISHED(TASKS),
  TASK_CANCELED(TASKS),
  MFA_ENABLED(ACCOUNT_SECURITY),
  MFA_DISABLED(ACCOUNT_SECURITY),
  PASSWORD_CHANGED(ACCOUNT_SECURITY),
  COMMENT_MENTION(COMMENTS, emailTiming = MENTION_DELAYED),
  KEYS_ADDED(LOCALIZATION, grouped = true, countable = true, emailTiming = WORK_GROUPED, entityKind = KEY),
  SOURCE_CHANGED(
    LOCALIZATION,
    grouped = true,
    countable = true,
    defaultEmail = false,
    emailTiming = WORK_GROUPED,
    entityKind = KEY,
  ),
  STRINGS_TRANSLATED(
    LOCALIZATION,
    grouped = true,
    countable = true,
    defaultEmail = false,
    emailTiming = WORK_GROUPED,
    entityKind = TRANSLATION,
  ),
  STRINGS_REVIEWED(
    LOCALIZATION,
    grouped = true,
    countable = true,
    defaultEmail = false,
    emailTiming = WORK_GROUPED,
    entityKind = TRANSLATION,
  ),
  AUTOMATICALLY_TRANSLATED(LOCALIZATION, grouped = true, emailTiming = WORK_GROUPED, entityKind = TRANSLATION),
  ;

  companion object {
    val GROUPED = entries.filter { it.grouped }

    fun withEmailTiming(timing: NotificationEmailTiming) = entries.filter { it.emailTiming == timing }

    fun withEntityKind(kind: NotificationEntityKind) = entries.filter { it.entityKind == kind }
  }
}
