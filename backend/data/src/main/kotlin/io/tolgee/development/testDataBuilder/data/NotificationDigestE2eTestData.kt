package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.key.Key
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.model.translation.Translation

class NotificationDigestE2eTestData : BaseTestData("notification_digest_user", "Digest project") {
  lateinit var translatedNotification: Notification
  lateinit var keysAddedNotification: Notification
  lateinit var key: Key
  lateinit var frenchTranslation: Translation

  init {
    projectBuilder.apply {
      addLanguage {
        name = "French"
        tag = "fr"
        originalName = "Français"
        flagEmoji = "🇫🇷"
      }
      key =
        addKey("digest-key")
          .apply {
            addTranslation("en", "Hello")
            frenchTranslation = addTranslation("fr", "Bonjour").self
          }.self
    }
    keysAddedNotification =
      userAccountBuilder
        .addNotification {
          user = this@NotificationDigestE2eTestData.user
          project = this@NotificationDigestE2eTestData.project
          type = NotificationType.KEYS_ADDED
        }.self
    translatedNotification =
      userAccountBuilder
        .addNotification {
          user = this@NotificationDigestE2eTestData.user
          project = this@NotificationDigestE2eTestData.project
          type = NotificationType.BULK_CHANGED
        }.self
  }
}
