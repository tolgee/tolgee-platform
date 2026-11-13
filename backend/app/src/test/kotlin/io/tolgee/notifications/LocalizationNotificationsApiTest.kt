package io.tolgee.notifications

import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationDigestFrequency
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationDigestStateService
import io.tolgee.service.notification.NotificationSettingsService
import io.tolgee.service.notification.activity.GroupedNotificationWrite
import io.tolgee.service.notification.activity.GroupedNotificationWriter
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.ObjectMapper
import java.util.Date

class LocalizationNotificationsApiTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var writer: GroupedNotificationWriter

  @Autowired
  private lateinit var settingsService: NotificationSettingsService

  @Autowired
  private lateinit var digestStateService: NotificationDigestStateService

  private lateinit var testData: NotificationRecipientsTestData

  @BeforeEach
  fun setup() {
    testData = NotificationRecipientsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.reviewerAll
  }

  private fun write(
    type: NotificationType,
    ids: List<Long>,
  ) = executeInNewTransaction(platformTransactionManager) {
    writer.write(
      GroupedNotificationWrite(testData.reviewerAll.id, testData.project.id, testData.author.id, type, ids, true),
    )
  }

  @Test
  fun `list shows count and languages`() {
    write(NotificationType.STRINGS_TRANSLATED, listOf(testData.existingFrench.id, testData.existingGerman.id))
    performAuthGet("/v2/notification").andIsOk.andAssertThatJson {
      node("_embedded.notificationModelList[0].type").isEqualTo("STRINGS_TRANSLATED")
      node("_embedded.notificationModelList[0].entityCount").isEqualTo(2)
      node("_embedded.notificationModelList[0].languages").isArray.hasSize(2)
    }
  }

  @Test
  fun `automatically translated shows languages and no count`() {
    write(NotificationType.AUTOMATICALLY_TRANSLATED, listOf(testData.existingFrench.id))
    performAuthGet("/v2/notification").andIsOk.andAssertThatJson {
      node("_embedded.notificationModelList[0].entityCount").isNull()
      node("_embedded.notificationModelList[0].languages[0].tag").isEqualTo("fr")
      node("_embedded.notificationModelList[0].branches").isArray.isEmpty()
    }
  }

  @Test
  fun `new keys show the first two keys with their base text`() {
    write(NotificationType.KEYS_ADDED, listOf(testData.existingKey.id, testData.branchKey.id, 1L))
    performAuthGet("/v2/notification").andIsOk.andAssertThatJson {
      node("_embedded.notificationModelList[0].entityCount").isEqualTo(3)
      node("_embedded.notificationModelList[0].sampleKeys").isArray.hasSize(2)
      node("_embedded.notificationModelList[0].sampleKeys[0].name").isEqualTo("existing-key")
      node("_embedded.notificationModelList[0].sampleKeys[0].baseText").isEqualTo("Hello")
      node("_embedded.notificationModelList[0].sampleKeys[1].name").isEqualTo("branch-key")
    }
  }

  @Test
  fun `only new keys carry key samples`() {
    write(NotificationType.SOURCE_CHANGED, listOf(testData.existingKey.id))
    performAuthGet("/v2/notification").andIsOk.andAssertThatJson {
      node("_embedded.notificationModelList[0].sampleKeys").isNull()
    }
  }

  @Test
  fun `non-default branches are listed, the default branch is not`() {
    write(NotificationType.STRINGS_TRANSLATED, listOf(testData.existingFrench.id, testData.branchFrench.id))
    Thread.sleep(5)
    write(NotificationType.KEYS_ADDED, listOf(testData.branchKey.id))
    performAuthGet("/v2/notification").andIsOk.andAssertThatJson {
      node("_embedded.notificationModelList[0].type").isEqualTo("KEYS_ADDED")
      node("_embedded.notificationModelList[0].branches").isEqualTo(listOf("feature-login"))
      node("_embedded.notificationModelList[1].branches").isEqualTo(listOf("feature-login"))
    }
  }

  @Test
  fun `list is ordered by last update`() {
    val older = write(NotificationType.KEYS_ADDED, listOf(1L))
    write(NotificationType.SOURCE_CHANGED, listOf(2L))
    Thread.sleep(5)
    write(NotificationType.KEYS_ADDED, listOf(3L))
    performAuthGet("/v2/notification").andIsOk.andAssertThatJson {
      node("_embedded.notificationModelList[0].id").isEqualTo(older.id)
    }
  }

  @Test
  fun `cursor follows update time and not creation time`() {
    val base = Date().time
    setForcedDate(Date(base))
    val a = write(NotificationType.KEYS_ADDED, listOf(1L))
    setForcedDate(Date(base + 1000))
    val b = write(NotificationType.SOURCE_CHANGED, listOf(2L))
    setForcedDate(Date(base + 2000))
    write(NotificationType.KEYS_ADDED, listOf(3L))
    clearForcedDate()

    val first = performAuthGet("/v2/notification?size=1").andIsOk
    first.andAssertThatJson { node("_embedded.notificationModelList[0].id").isEqualTo(a.id) }
    val cursor = readCursor(first)
    val second = performAuthGet("/v2/notification?size=1&cursor=$cursor").andIsOk
    second.andAssertThatJson { node("_embedded.notificationModelList[0].id").isEqualTo(b.id) }
    val third = performAuthGet("/v2/notification?size=1&cursor=${readCursor(second)}").andIsOk
    third.andAssertThatJson { node("_embedded.notificationModelList").isAbsent() }
  }

  private fun readCursor(result: org.springframework.test.web.servlet.ResultActions): String =
    ObjectMapper().readValue(result.andReturn().response.contentAsString, Map::class.java)["nextCursor"] as String

  @Test
  fun `marking seen keeps the work email limit`() {
    val n = write(NotificationType.KEYS_ADDED, listOf(1L))
    digestStateService.markSent(testData.reviewerAll.id, Date())
    performAuthPut("/v2/notifications-mark-seen", mapOf("notificationIds" to listOf(n.id))).andIsOk
    digestStateService
      .find(listOf(testData.reviewerAll.id))[testData.reviewerAll.id]!!
      .lastDigestSentAt.assert
      .isNotNull()
  }

  @Test
  fun `settings list localization types and save per type`() {
    performAuthGet("/v2/notification-settings").andIsOk.andAssertThatJson {
      node("localization").isArray.hasSize(NotificationType.GROUPED.size)
      node("digestFrequency").isEqualTo("SOON")
    }
    performAuthPut(
      "/v2/notification-settings",
      mapOf("group" to "LOCALIZATION", "type" to "STRINGS_REVIEWED", "channel" to "EMAIL", "enabled" to true),
    ).andIsOk
    settingsService
      .isEnabled(testData.reviewerAll.id, NotificationType.STRINGS_REVIEWED, NotificationChannel.EMAIL)
      .assert
      .isTrue()
  }

  @Test
  fun `localization setting without type is rejected`() {
    performAuthPut(
      "/v2/notification-settings",
      mapOf("group" to "LOCALIZATION", "channel" to "EMAIL", "enabled" to true),
    ).andIsBadRequest
  }

  @Test
  fun `digest frequency can be turned off`() {
    performAuthPut("/v2/notification-settings/digest-frequency", mapOf("frequency" to "OFF")).andIsOk
    digestStateService.getFrequency(testData.reviewerAll.id).assert.isEqualTo(NotificationDigestFrequency.OFF)
  }
}
