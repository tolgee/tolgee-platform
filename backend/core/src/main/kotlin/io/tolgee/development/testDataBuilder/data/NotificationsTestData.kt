package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.notifications.Notification

class NotificationsTestData : BaseTestData("test_user@example.com") {
  private var currentTaskNumber = 1L

  val originatingUser =
    root.addUserAccount {
      name = "originating user"
      username = "notificationsOriginatingUser"
    }

  fun generateNotificationWithTask(taskNumber: Long = currentTaskNumber++): Notification {
    val task =
      projectBuilder.addTask {
        this.name = "Notification task $taskNumber"
        this.language = englishLanguage
        this.author = originatingUser.self
        this.number = taskNumber
      }

    val notification =
      userAccountBuilder.addNotification {
        this.user = this@NotificationsTestData.user
        this.project = this@NotificationsTestData.project
        this.linkedTask = task.self
        this.originatingUser = this@NotificationsTestData.originatingUser.self
      }

    return notification.self
  }
}
