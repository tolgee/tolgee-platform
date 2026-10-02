package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.model.translation.Translation

class NotificationDigestE2eTestData : BaseTestData("notification_digest_user", "Digest project") {
  lateinit var translatedNotification: Notification
  lateinit var frenchTranslation: Translation

  init {
    projectBuilder.apply {
      addLanguage {
        name = "French"
        tag = "fr"
        originalName = "Français"
        flagEmoji = "🇫🇷"
      }
      addKey("digest-key").apply {
        addTranslation("en", "Hello")
        frenchTranslation = addTranslation("fr", "Bonjour").self
      }
    }
    userAccountBuilder.addNotification {
      user = this@NotificationDigestE2eTestData.user
      project = this@NotificationDigestE2eTestData.project
      type = NotificationType.AUTOMATICALLY_TRANSLATED
    }
    translatedNotification =
      userAccountBuilder
        .addNotification {
          user = this@NotificationDigestE2eTestData.user
          project = this@NotificationDigestE2eTestData.project
          type = NotificationType.BULK_CHANGED
        }.self
  }
}
