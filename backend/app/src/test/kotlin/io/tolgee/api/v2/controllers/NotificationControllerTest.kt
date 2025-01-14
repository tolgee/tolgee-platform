package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.NotificationsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test

class NotificationControllerTest : AuthorizedControllerTest() {
  @Test
  fun `gets notifications from newest`() {
    val testData = NotificationsTestData()

    (101L..103).forEach { i ->
      executeInNewTransaction {
        val task =
          testData.projectBuilder.addTask {
            this.name = "Notification task $i"
            this.language = testData.englishLanguage
            this.author = testData.originatingUser.self
            this.number = i
          }

        testData.userAccountBuilder.addNotification {
          this.user = testData.user
          this.project = testData.project
          this.linkedTask = task.self
          this.originatingUser = testData.originatingUser.self
        }
      }
    }

    testDataService.saveTestData(testData.root)
    loginAsUser(testData.user.username)

    performAuthGet("/v2/notifications").andAssertThatJson {
      node("_embedded.notificationModelList[0].linkedTask.name").isEqualTo("Notification task 103")
      node("_embedded.notificationModelList[1].linkedTask.name").isEqualTo("Notification task 102")
      node("_embedded.notificationModelList[2].linkedTask.name").isEqualTo("Notification task 101")
    }
  }
}
