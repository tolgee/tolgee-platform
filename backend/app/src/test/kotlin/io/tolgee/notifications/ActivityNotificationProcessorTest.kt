package io.tolgee.notifications

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.LockingProvider
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.service.notification.NotificationSettingsService
import io.tolgee.service.notification.activity.ActivityNotificationProcessor
import io.tolgee.service.notification.activity.ActivityNotificationQueue
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration

class ActivityNotificationProcessorTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var processor: ActivityNotificationProcessor

  @Autowired
  private lateinit var queue: ActivityNotificationQueue

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Autowired
  private lateinit var settingsService: NotificationSettingsService

  @Autowired
  private lateinit var lockingProvider: LockingProvider

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var testData: NotificationRecipientsTestData

  @BeforeEach
  fun setup() {
    testData = NotificationRecipientsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.author
    projectSupplier = { testData.project }
    jdbcTemplate.update("delete from notification_activity_queue")
  }

  private fun insertFailingMarker() {
    jdbcTemplate.update(
      "insert into notification_activity_queue (activity_revision_id, attempts, created_at) values (?, 0, now())",
      -1L,
    )
    jdbcTemplate.update(
      "insert into activity_revision (id, timestamp, project_id) values (-1, now(), ?)",
      testData.project.id,
    )
    jdbcTemplate.update(
      """
      insert into activity_modified_entity (activity_revision_id, entity_class, entity_id, revision_type, modifications, describing_relations)
      values (-1, 'Translation', 1, 1, '{"state": {"old": "TRANSLATED", "new": "REVIEWED"}}', '{"language": {"entityClass": "Language", "entityId": "not-a-number"}}')
      """,
    )
  }

  private fun notificationsOf(type: NotificationType) = notificationRepository.findAll().filter { it.type == type }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creating keys notifies translators once per user and empties the queue`() {
    performProjectAuthPost("keys", mapOf("name" to "k1")).andIsCreated
    performProjectAuthPost("keys", mapOf("name" to "k2")).andIsCreated
    processor.processQueue()
    val rows = notificationsOf(NotificationType.KEYS_ADDED)
    rows.map { it.user.id }.assert.containsExactlyInAnyOrder(
      testData.translatorFr.id,
      testData.reviewerFr.id,
      testData.reviewerAll.id,
    )
    notificationsOf(NotificationType.KEYS_ADDED)
      .single { it.user.id == testData.reviewerAll.id }
      .emailPending.assert
      .isTrue()
    queue.findAll().assert.isEmpty()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `in-app disabled type creates no row for that user`() {
    settingsService.saveForType(testData.reviewerAll, NotificationType.KEYS_ADDED, NotificationChannel.IN_APP, false)
    performProjectAuthPost("keys", mapOf("name" to "k1")).andIsCreated
    processor.processQueue()
    notificationsOf(NotificationType.KEYS_ADDED).map { it.user.id }.assert.doesNotContain(testData.reviewerAll.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `digest-disabled type creates a row without email pending`() {
    performProjectAuthPut("translations/${testData.existingFrench.id}/set-state/REVIEWED").andIsOk
    settingsService.saveForType(testData.viewerDe, NotificationType.STRINGS_REVIEWED, NotificationChannel.EMAIL, false)
    settingsService.saveForType(
      testData.reviewerAll,
      NotificationType.STRINGS_REVIEWED,
      NotificationChannel.EMAIL,
      false,
    )
    processor.processQueue()
    notificationsOf(NotificationType.STRINGS_REVIEWED)
      .single { it.user.id == testData.reviewerAll.id }
      .emailPending.assert
      .isFalse()
  }

  @Test
  fun `marker of a missing revision is deleted in a single run`() {
    queue.add(Long.MAX_VALUE)
    processor.processQueue()
    queue.findAll().assert.isEmpty()
  }

  @Test
  fun `failing marker is retried once per run, then dropped`() {
    insertFailingMarker()
    processor.processQueue()
    jdbcTemplate
      .queryForObject(
        "select attempts from notification_activity_queue where activity_revision_id = -1",
        Int::class.java,
      ).assert
      .isEqualTo(1)
    repeat(4) { processor.processQueue() }
    queue.findAll().assert.doesNotContain(-1L)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `markers of one project are merged into one write per recipient`() {
    repeat(3) { performProjectAuthPost("keys", mapOf("name" to "merged-$it")).andIsCreated }
    queue.size().assert.isEqualTo(3)
    processor.processQueue()
    val row = notificationsOf(NotificationType.KEYS_ADDED).single { it.user.id == testData.reviewerAll.id }
    jdbcTemplate
      .queryForObject("select count(*) from notification_entity where notification_id = ?", Int::class.java, row.id)
      .assert
      .isEqualTo(3)
    queue.size().assert.isEqualTo(0)
    queue.oldestAgeSeconds().assert.isEqualTo(0)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `failing marker does not block other markers of the same project`() {
    insertFailingMarker()
    performProjectAuthPost("keys", mapOf("name" to "good-key")).andIsCreated
    processor.processQueue()
    notificationsOf(NotificationType.KEYS_ADDED).map { it.user.id }.assert.contains(testData.reviewerAll.id)
    queue.findAll().assert.containsExactly(-1L)
    jdbcTemplate
      .queryForObject(
        "select attempts from notification_activity_queue where activity_revision_id = -1",
        Int::class.java,
      ).assert
      .isEqualTo(1)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does nothing when another node holds the lock`() {
    performProjectAuthPost("keys", mapOf("name" to "k1")).andIsCreated
    lockingProvider.withLockingIfFree("notification-activity-processing", Duration.ofMinutes(1)) {
      Thread { processor.processQueue() }.apply {
        start()
        join()
      }
    }
    queue.findAll().assert.hasSize(1)
  }
}
