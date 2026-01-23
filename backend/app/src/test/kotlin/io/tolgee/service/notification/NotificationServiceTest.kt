package io.tolgee.service.notification

import io.tolgee.AbstractSpringTest
import io.tolgee.config.TestEmailConfiguration
import io.tolgee.development.Base
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationType
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.testing.NotificationTestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(TestEmailConfiguration::class)
class NotificationServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var notificationTestUtil: NotificationTestUtil

  @Autowired
  private lateinit var notificationService: NotificationService

  @Autowired
  private lateinit var notificationSettingsService: NotificationSettingsService

  private lateinit var base: Base

  @BeforeEach
  fun setUp() {
    notificationTestUtil.init()
    base = dbPopulator.createBase()
  }

  @Test
  fun `in-app and email notification are created`() {
    notifyUser()
    assertInAppNotificationExists()
    assertEmailNotificationExists()
  }

  @Test
  fun `in-app notification is not created when disabled`() {
    disableChannel(NotificationChannel.IN_APP)
    notifyUser()
    assertInAppNotificationNotExists()
    assertEmailNotificationExists()
  }

  @Test
  fun `email notification is not created when disabled`() {
    disableChannel(NotificationChannel.EMAIL)
    notifyUser()
    assertInAppNotificationExists()
    assertEmailNotificationNotExists()
  }

  private fun disableChannel(channel: NotificationChannel) {
    notificationSettingsService.save(
      base.userAccount,
      NotificationTypeGroup.ACCOUNT_SECURITY,
      channel,
      false,
    )
  }

  private fun notifyUser() {
    notificationService.notify(
      Notification().apply {
        this.user = base.userAccount
        this.type = NotificationType.PASSWORD_CHANGED
      },
    )
  }

  private fun assertInAppNotificationExists() {
    notificationTestUtil.newestInAppNotification().also {
      assertThat(it.user.id).isEqualTo(base.userAccount.id)
      assertThat(it.type).isEqualTo(NotificationType.PASSWORD_CHANGED)
    }
  }

  private fun assertInAppNotificationNotExists() = notificationTestUtil.assertNoInAppNotifications()

  private fun assertEmailNotificationExists() {
    waitForNotThrowing(timeout = 2000, pollTime = 25) {
      notificationTestUtil.newestEmailNotification().also {
        assertThat(it).contains("Password has been changed for your account")
      }
    }
  }

  private fun assertEmailNotificationNotExists() = notificationTestUtil.assertNoEmailNotifications()
}
