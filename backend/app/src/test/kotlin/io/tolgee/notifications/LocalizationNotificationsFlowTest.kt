package io.tolgee.notifications

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.fixtures.AuthorizedRequestFactory
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.service.notification.activity.ActivityNotificationProcessor
import io.tolgee.service.notification.digest.NotificationDigestJob
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.time.Duration
import java.util.Date

class LocalizationNotificationsFlowTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var processor: ActivityNotificationProcessor

  @Autowired
  private lateinit var digestJob: NotificationDigestJob

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var testData: NotificationRecipientsTestData

  @BeforeEach
  fun setup() {
    emailTestUtil.initMocks()
    testData = NotificationRecipientsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.author
    projectSupplier = { testData.project }
    jdbcTemplate.update("delete from notification_activity_queue")
    setForcedDate(Date())
  }

  @AfterEach
  fun after() = clearForcedDate()

  @Test
  @ProjectJWTAuthTestMethod
  fun `import of many keys creates one capped row per recipient and one digest each`() {
    val json = (1..1500).joinToString(",", "{", "}") { "\"key$it\": \"text $it\"" }
    importJson(json).andIsOk
    processor.processQueue()

    val all = notificationRepository.findAll()
    all.groupBy { it.user.id to it.type }.forEach { (_, rows) -> rows.assert.hasSize(1) }
    val keysAdded = all.filter { it.type == NotificationType.KEYS_ADDED }
    keysAdded.map { it.user.id }.assert.containsExactlyInAnyOrder(
      testData.translatorFr.id,
      testData.reviewerFr.id,
      testData.reviewerAll.id,
    )
    keysAdded.forEach {
      jdbcTemplate
        .queryForObject(
          "select count(*) from notification_entity where notification_id = ?",
          Int::class.java,
          it.id,
        ).assert
        .isEqualTo(100)
    }

    digestJob.sendDueDigests()
    emailTestUtil.verifyTimesEmailSent(0)

    moveCurrentDate(Duration.ofMinutes(11))
    digestJob.sendDueDigests()
    emailTestUtil.verifyTimesEmailSent(3)
    emailTestUtil.messageArgumentCaptor.allValues
      .map { it.getHeader("To")[0] }
      .assert
      .containsExactlyInAnyOrder(
        testData.translatorFr.username,
        testData.reviewerFr.username,
        testData.reviewerAll.username,
      )

    digestJob.sendDueDigests()
    emailTestUtil.verifyTimesEmailSent(3)
  }

  private fun importJson(json: String): ResultActions {
    val builder =
      MockMvcRequestBuilders
        .multipart("/v2/projects/${testData.project.id}/single-step-import")
    builder.file(MockMultipartFile("files", "en.json", "application/json", json.toByteArray()))
    builder.part(MockPart("params", "{}".toByteArray()))
    return mvc.perform(AuthorizedRequestFactory.addToken(builder))
  }
}
