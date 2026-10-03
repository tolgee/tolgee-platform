package io.tolgee.notifications

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.service.notification.NotificationCleanupJob
import io.tolgee.service.notification.activity.GroupedNotificationWriter
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class NotificationCleanupJobTest : AbstractSpringTest() {
  @Autowired
  private lateinit var job: NotificationCleanupJob

  @Autowired
  private lateinit var writer: GroupedNotificationWriter

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Test
  fun `deletes seen rows older than 7 days with their entities, keeps the rest`() {
    val testData = NotificationRecipientsTestData()
    testDataService.saveTestData(testData.root)

    fun write(type: NotificationType) =
      executeInNewTransaction(platformTransactionManager) {
        writer.write(testData.reviewerAll.id, testData.project.id, null, type, listOf(1L), true)
      }
    val oldSeen = write(NotificationType.KEYS_ADDED)
    val recentSeen = write(NotificationType.SOURCE_CHANGED)
    val oldUnseen = write(NotificationType.STRINGS_REVIEWED)
    val oldJustSeen = write(NotificationType.STRINGS_TRANSLATED)
    jdbcTemplate.update(
      "update notification set seen = true, seen_at = now() - interval '8 days' where id = ?",
      oldSeen.id,
    )
    jdbcTemplate.update("update notification set seen = true, seen_at = now() where id = ?", recentSeen.id)
    jdbcTemplate.update("update notification set updated_at = now() - interval '8 days' where id = ?", oldUnseen.id)
    jdbcTemplate.update(
      "update notification set seen = true, seen_at = now(), updated_at = now() - interval '8 days' where id = ?",
      oldJustSeen.id,
    )

    job.deleteOldSeen()

    notificationRepository
      .findAllById(listOf(oldSeen.id, recentSeen.id, oldUnseen.id, oldJustSeen.id))
      .map { it.id }
      .assert
      .containsExactlyInAnyOrder(recentSeen.id, oldUnseen.id, oldJustSeen.id)
    jdbcTemplate
      .queryForObject(
        "select count(*) from notification_entity where notification_id = ?",
        Int::class.java,
        oldSeen.id,
      ).assert
      .isEqualTo(0)
  }

  @Test
  fun `deletes more rows than one batch holds`() {
    val testData = NotificationRecipientsTestData()
    testDataService.saveTestData(testData.root)
    val template =
      executeInNewTransaction(platformTransactionManager) {
        writer.write(testData.reviewerAll.id, testData.project.id, null, NotificationType.KEYS_ADDED, listOf(1L), true)
      }
    jdbcTemplate.update(
      """
      insert into notification (id, user_id, project_id, type, seen, seen_at, email_pending, created_at, updated_at)
      select -g, user_id, project_id, type, true, now() - interval '8 days', false, created_at, updated_at
      from notification, generate_series(1, 2500) g
      where id = ?
      """,
      template.id,
    )

    job.deleteOldSeen()

    jdbcTemplate
      .queryForObject("select count(*) from notification where id < 0", Int::class.java)
      .assert
      .isEqualTo(0)
    notificationRepository.existsById(template.id).assert.isTrue()
  }
}
