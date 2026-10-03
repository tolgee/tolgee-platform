package io.tolgee.service.notification

import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class EmailNotificationComposer(
  private val i18n: I18n,
  private val taskEmailComposer: TaskEmailComposer,
  private val mfaEmailComposer: MfaEmailComposer,
  private val passwordChangedEmailComposer: PasswordChangedEmailComposer,
) {
  fun composeEmailSubject(notification: Notification) =
    i18n.translate("notifications.email.subject.${notification.type}")

  fun composeEmailText(notification: Notification) =
    when (notification.type) {
      NotificationType.TASK_ASSIGNED,
      NotificationType.TASK_FINISHED,
      NotificationType.TASK_CANCELED,
      -> taskEmailComposer

      NotificationType.MFA_ENABLED,
      NotificationType.MFA_DISABLED,
      -> mfaEmailComposer

      NotificationType.PASSWORD_CHANGED,
      -> passwordChangedEmailComposer

      NotificationType.KEYS_ADDED,
      NotificationType.SOURCE_CHANGED,
      NotificationType.STRINGS_TRANSLATED,
      NotificationType.STRINGS_REVIEWED,
      NotificationType.AUTOMATICALLY_TRANSLATED,
      NotificationType.BULK_CHANGED,
      -> throw IllegalStateException("No email composer for notification type ${notification.type}")
    }.composeEmail(notification)
}
