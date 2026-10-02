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
    jdbcTemplate.update(
      "update notification set seen = true, updated_at = now() - interval '8 days' where id = ?",
      oldSeen.id,
    )
    jdbcTemplate.update("update notification set seen = true where id = ?", recentSeen.id)
    jdbcTemplate.update("update notification set updated_at = now() - interval '8 days' where id = ?", oldUnseen.id)

    job.deleteOldSeen()

    notificationRepository
      .findAllById(listOf(oldSeen.id, recentSeen.id, oldUnseen.id))
      .map { it.id }
      .assert
      .containsExactlyInAnyOrder(recentSeen.id, oldUnseen.id)
    jdbcTemplate
      .queryForObject(
        "select count(*) from notification_entity where notification_id = ?",
        Int::class.java,
        oldSeen.id,
      ).assert
      .isEqualTo(0)
  }
}
