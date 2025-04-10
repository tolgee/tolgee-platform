package io.tolgee.api.v2.controllers.notification

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.development.testDataBuilder.data.NotificationsTestData
import io.tolgee.dtos.request.notification.NotificationsMarkSeenRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.testing.AuthorizedControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.ResultActions

class NotificationControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var notificationRepository: NotificationRepository

  @Test
  fun `gets notifications from newest`() {
    val testData = NotificationsTestData()

    (101L..103).forEach { taskNumber ->
      testData.generateNotificationWithTask(taskNumber)
    }
    testData.generateNotificationWithTask(104).seen = true

    testDataService.saveTestData(testData.root)
    loginAsUser(testData.user.username)

    performAuthGet("/v2/notification?filterSeen=false").andAssertThatJson {
      node("_embedded.notificationModelList").isArray.hasSize(3)
      node("_embedded.notificationModelList[0].linkedTask.name").isEqualTo("Notification task 103")
      node("_embedded.notificationModelList[1].linkedTask.name").isEqualTo("Notification task 102")
      node("_embedded.notificationModelList[2].linkedTask.name").isEqualTo("Notification task 101")
    }

    performAuthGet("/v2/notification").andAssertThatJson {
      node("_embedded.notificationModelList").isArray.hasSize(4)
      node("_embedded.notificationModelList[0].linkedTask.name").isEqualTo("Notification task 104")
      node("_embedded.notificationModelList[0].originatingUser.name").isEqualTo("originating user")
    }
  }

  @Test
  fun `gets notifications by cursor`() {
    val testData = NotificationsTestData()

    (101L..205).forEach { taskNumber ->
      testData.generateNotificationWithTask(taskNumber)
    }

    testDataService.saveTestData(testData.root)
    loginAsUser(testData.user.username)

    var nextCursor: String? = null

    (0..9).forEach { i ->
      getNotificationsByCursor(nextCursor)
        .andAssertThatJson {
          node("_embedded.notificationModelList").isArray.hasSize(10)
          node("_embedded.notificationModelList[0].linkedTask.name").isEqualTo("Notification task ${205 - 10 * i}")
          node("_embedded.notificationModelList[9].linkedTask.name").isEqualTo("Notification task ${205 - 10 * i - 9}")
          node("nextCursor").isNotNull
        }.andDo {
          nextCursor = ObjectMapper().readValue(it.response.contentAsString, Map::class.java)["nextCursor"] as String
        }
    }

    getNotificationsByCursor(nextCursor).andAssertThatJson {
      node("_embedded.notificationModelList").isArray.hasSize(5)
      node("nextCursor").isNull()
    }
  }

  private fun getNotificationsByCursor(cursor: String?): ResultActions {
    val cursorQuery = cursor?.let { "&cursor=$it" } ?: ""
    return performAuthGet("/v2/notification?size=10$cursorQuery")
  }

  @Test
  fun `marks notifications as seen`() {
    val testData = NotificationsTestData()
    val currentUserNotification1 = testData.generateNotificationWithTask(101)
    val currentUserNotification2 = testData.generateNotificationWithTask(102)
    val differentUserNotification =
      testData.generateNotificationWithTask(103).apply {
        user = testData.root.addUserAccountWithoutOrganization { username = "Different User" }.self
      }

    testDataService.saveTestData(testData.root)
    loginAsUser(testData.user.username)

    performAuthPut(
      "/v2/notifications-mark-seen",
      NotificationsMarkSeenRequest().apply {
        notificationIds =
          listOf(
            currentUserNotification1.id,
            currentUserNotification2.id,
            differentUserNotification.id,
          )
      },
    ).andIsOk

    val notifications = notificationRepository.findAll()

    assertThat(notifications.find { it.id == currentUserNotification1.id }?.seen).isTrue()
    assertThat(notifications.find { it.id == currentUserNotification2.id }?.seen).isTrue()
    assertThat(notifications.find { it.id == differentUserNotification.id }?.seen).isFalse()
  }
}
