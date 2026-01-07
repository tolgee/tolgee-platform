package io.tolgee.api.v2.controllers.notification

import io.tolgee.config.TestEmailConfiguration
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.dtos.request.notification.NotificationSettingsRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationType
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.service.notification.NotificationService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.NotificationTestUtil
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(TestEmailConfiguration::class)
class NotificationSettingsControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var notificationService: NotificationService

  @Autowired
  lateinit var notificationTestUtil: NotificationTestUtil

  lateinit var testData: TaskTestData

  @BeforeEach
  fun setUp() {
    notificationTestUtil.init()
    testData = TaskTestData()
    testDataService.saveTestData(testData.root)
    loginAsUser(testData.user.username)
  }

  @Test
  fun `gets default notification settings`() {
    performAuthGet("/v2/notification-settings").andAssertThatJson {
      node("accountSecurity") {
        node("inApp").isEqualTo(true)
        node("email").isEqualTo(true)
      }
      node("tasks") {
        node("inApp").isEqualTo(true)
        node("email").isEqualTo(true)
      }
    }
  }

  @Test
  fun `gets changed notification settings`() {
    disableChannel(NotificationChannel.IN_APP)
    disableChannel(NotificationChannel.EMAIL)

    performAuthGet("/v2/notification-settings").andAssertThatJson {
      node("accountSecurity") {
        node("inApp").isEqualTo(true)
        node("email").isEqualTo(true)
      }
      node("tasks") {
        node("inApp").isEqualTo(false)
        node("email").isEqualTo(false)
      }
    }
  }

  @Test
  fun `notification gets dispatched if enabled`() {
    val notification = dispatchNotification()

    assertThat(notificationTestUtil.newestInAppNotification().id).isEqualTo(notification.id)
    assertThat(
      notificationTestUtil.newestEmailNotification(),
    ).contains("projects/${testData.project.id}/task?number=${testData.translateTask.self.number}")
  }

  @Test
  fun `notification does not get dispatched if disabled`() {
    disableChannel(NotificationChannel.IN_APP)
    disableChannel(NotificationChannel.EMAIL)

    dispatchNotification()

    notificationTestUtil.assertNoInAppNotifications()
    notificationTestUtil.assertNoEmailNotifications()
  }

  private fun disableChannel(channel: NotificationChannel) {
    performAuthPut(
      "/v2/notification-settings",
      content = NotificationSettingsRequest(NotificationTypeGroup.TASKS, channel, false),
    ).andIsOk
  }

  private fun dispatchNotification(): Notification =
    Notification()
      .apply {
        type = NotificationType.TASK_ASSIGNED
        user = testData.user
        linkedTask = testData.translateTask.self
        project = testData.project
        originatingUser = testData.user
      }.also {
        notificationService.notify(it)
      }
}
