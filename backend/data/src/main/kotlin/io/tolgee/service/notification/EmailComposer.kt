package io.tolgee.service.notification

import io.tolgee.model.notifications.Notification

interface EmailComposer {
  fun composeEmail(notification: Notification): String
}
