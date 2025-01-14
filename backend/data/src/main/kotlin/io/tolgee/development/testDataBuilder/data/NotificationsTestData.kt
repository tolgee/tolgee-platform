package io.tolgee.development.testDataBuilder.data

class NotificationsTestData : BaseTestData() {
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
}
