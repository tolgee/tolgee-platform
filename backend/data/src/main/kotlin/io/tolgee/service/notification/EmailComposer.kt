package io.tolgee.service.notification

import io.tolgee.model.Notification

interface EmailComposer {
  fun composeEmail(notification: Notification): String
}
