package io.tolgee.notifications

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationDigestFrequency
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.service.notification.NotificationDigestStateService
import io.tolgee.service.notification.NotificationSettingsService
import io.tolgee.service.notification.activity.GroupedNotificationWriter
import io.tolgee.service.notification.digest.NotificationDigestJob
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import java.util.Date

class NotificationDigestJobTest : AbstractSpringTest() {
  @Autowired
  private lateinit var job: NotificationDigestJob

  @Autowired
  private lateinit var writer: GroupedNotificationWriter

  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  @Autowired
  private lateinit var digestStateService: NotificationDigestStateService

  @Autowired
  private lateinit var settingsService: NotificationSettingsService

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var testData: NotificationRecipientsTestData

  @BeforeEach
  fun setup() {
    emailTestUtil.initMocks()
    testData = NotificationRecipientsTestData()
    testDataService.saveTestData(testData.root)
    setForcedDate(Date())
  }

  @AfterEach
  fun cleanup() {
    clearForcedDate()
  }

  private fun write(
    type: NotificationType = NotificationType.KEYS_ADDED,
    ids: List<Long> = listOf(1L),
  ) = executeInNewTransaction(platformTransactionManager) {
    writer.write(testData.reviewerAll.id, testData.project.id, testData.author.id, type, ids, true)
  }

  private fun afterGrace() = moveCurrentDate(Duration.ofMinutes(11))

  @Test
  fun `nothing is sent inside the grace period`() {
    write()
    job.sendDueDigests()
    emailTestUtil.messageContents.assert.isEmpty()
  }

  @Test
  fun `one digest per user after the grace period, then nothing for 24 h`() {
    write(NotificationType.KEYS_ADDED, listOf(1L, 2L))
    write(NotificationType.SOURCE_CHANGED, listOf(3L))
    afterGrace()
    job.sendDueDigests()
    emailTestUtil.verifyTimesEmailSent(1)
    emailTestUtil.singleEmailContent.assert
      .contains("Notification project")
      .contains("New keys added")

    write(NotificationType.KEYS_ADDED, listOf(4L))
    afterGrace()
    job.sendDueDigests()
    emailTestUtil.verifyTimesEmailSent(1)

    moveCurrentDate(Duration.ofHours(24))
    job.sendDueDigests()
    emailTestUtil.verifyTimesEmailSent(2)
  }

  @Test
  fun `viewing resets the window`() {
    write()
    afterGrace()
    job.sendDueDigests()
    digestStateService.resetWindow(testData.reviewerAll.id)
    write(NotificationType.SOURCE_CHANGED)
    afterGrace()
    job.sendDueDigests()
    emailTestUtil.verifyTimesEmailSent(2)
  }

  @Test
  fun `digest lists non-default branches`() {
    write(NotificationType.STRINGS_TRANSLATED, listOf(testData.branchFrench.id))
    afterGrace()
    job.sendDueDigests()
    emailTestUtil.singleEmailContent.assert.contains("feature-login")
  }

  @Test
  fun `frequency off sends nothing`() {
    digestStateService.setFrequency(testData.reviewerAll.id, NotificationDigestFrequency.OFF)
    write()
    afterGrace()
    job.sendDueDigests()
    emailTestUtil.messageContents.assert.isEmpty()
  }

  @Test
  fun `type disabled for digest after writing is skipped and cleared`() {
    val n = write()
    settingsService.saveForType(testData.reviewerAll, NotificationType.KEYS_ADDED, NotificationChannel.EMAIL, false)
    afterGrace()
    job.sendDueDigests()
    emailTestUtil.messageContents.assert.isEmpty()
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isFalse()
  }

  @Test
  fun `message id is the same after a crash before commit and changes with new content`() {
    val n = write()
    afterGrace()
    job.sendDueDigests()
    val first = messageId(0)
    first.assert.startsWith("<digest-${testData.reviewerAll.id}-")

    jdbcTemplate.update("update notification set email_pending = true where id = ?", n.id)
    jdbcTemplate.update(
      "update notification_digest_state set last_digest_sent_at = null where user_id = ?",
      testData.reviewerAll.id,
    )
    job.sendDueDigests()
    emailTestUtil.verifyTimesEmailSent(2)
    messageId(1).assert.isEqualTo(first)

    moveCurrentDate(Duration.ofHours(25))
    write(ids = listOf(5L))
    afterGrace()
    job.sendDueDigests()
    emailTestUtil.verifyTimesEmailSent(3)
    messageId(2).assert.isNotEqualTo(first)
  }

  @Test
  fun `content written during sending keeps the row pending`() {
    val n = write()
    afterGrace()
    whenever(emailTestUtil.javaMailSender.send(any<MimeMessage>())).thenAnswer {
      moveCurrentDate(Duration.ofSeconds(1))
      write(ids = listOf(7L))
    }
    job.sendDueDigests()
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isTrue()
  }

  private fun messageId(index: Int) = emailTestUtil.messageArgumentCaptor.allValues[index].getHeader("Message-ID")[0]
}
