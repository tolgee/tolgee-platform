package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Notification

class NotificationsTestData : BaseTestData() {
  val originatingUser =
    root.addUserAccount {
      name = "originating user"
      username = "notificationsOriginatingUser"
    }

  val notification = generateNotificationWithTask(100)
  val task = notification.linkedTask

  fun generateNotificationWithTask(taskNumber: Long): Notification {
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
