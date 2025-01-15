package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Notification

class NotificationsTestData : BaseTestData() {
  private var nextTaskId = 100000L

  val originatingUser =
    root.addUserAccount {
      name = "originating user"
      username = "originatingUser"
    }

  val task =
    projectBuilder.addTask {
      this.name = "Notification task"
      this.language = englishLanguage
      this.author = originatingUser.self
    }

  val notificationBuilder =
    userAccountBuilder.addNotification {
      this.user = this@NotificationsTestData.user
      this.project = this@NotificationsTestData.project
      this.linkedTask = task.self
      this.originatingUser = this@NotificationsTestData.originatingUser.self
    }

  val notification = notificationBuilder.self

  fun generateNotificationWithTask(taskNumber: Long = nextTaskId++): Notification {
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
