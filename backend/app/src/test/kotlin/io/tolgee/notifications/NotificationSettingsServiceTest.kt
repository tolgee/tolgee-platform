package io.tolgee.notifications

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.NotificationsTestData
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationDigestFrequency
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationDigestStateService
import io.tolgee.service.notification.NotificationSettingsService
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

class NotificationSettingsServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var settingsService: NotificationSettingsService

  @Autowired
  private lateinit var digestStateService: NotificationDigestStateService

  private lateinit var testData: NotificationsTestData

  @BeforeEach
  fun setup() {
    testData = NotificationsTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `localization types use their own defaults`() {
    val userId = testData.user.id
    settingsService.isEnabled(userId, NotificationType.KEYS_ADDED, NotificationChannel.EMAIL).assert.isTrue()
    settingsService.isEnabled(userId, NotificationType.STRINGS_REVIEWED, NotificationChannel.EMAIL).assert.isFalse()
    settingsService.isEnabled(userId, NotificationType.STRINGS_REVIEWED, NotificationChannel.IN_APP).assert.isTrue()
    settingsService
      .isEnabled(userId, NotificationType.AUTOMATICALLY_TRANSLATED, NotificationChannel.EMAIL)
      .assert
      .isTrue()
    settingsService.isEnabled(userId, NotificationType.SOURCE_CHANGED, NotificationChannel.EMAIL).assert.isFalse()
    settingsService.isEnabled(userId, NotificationType.COMMENT_MENTION, NotificationChannel.EMAIL).assert.isTrue()
  }

  @Test
  fun `per type setting overrides the default`() {
    settingsService.saveForType(testData.user, NotificationType.KEYS_ADDED, NotificationChannel.IN_APP, false)
    settingsService
      .usersWithEnabled(
        listOf(testData.user.id, testData.originatingUser.self.id),
        NotificationType.KEYS_ADDED,
        NotificationChannel.IN_APP,
      ).assert
      .containsExactly(testData.originatingUser.self.id)
  }

  @Test
  fun `getSettings lists every localization type and channel`() {
    val settings = settingsService.getSettings(testData.user)
    val localization = settings.filter { it.type != null }
    localization.assert.hasSize(NotificationType.GROUPED.size * NotificationChannel.entries.size)
  }

  @Test
  fun `digest state defaults to soon and records the last work email`() {
    val userId = testData.user.id
    digestStateService.getFrequency(userId).assert.isEqualTo(NotificationDigestFrequency.SOON)
    digestStateService.markSent(userId, Date())
    digestStateService
      .find(listOf(userId))[userId]!!
      .lastDigestSentAt.assert
      .isNotNull()
    digestStateService.getFrequency(userId).assert.isEqualTo(NotificationDigestFrequency.SOON)
    digestStateService.setFrequency(userId, NotificationDigestFrequency.OFF)
    digestStateService.getFrequency(userId).assert.isEqualTo(NotificationDigestFrequency.OFF)
  }
}
