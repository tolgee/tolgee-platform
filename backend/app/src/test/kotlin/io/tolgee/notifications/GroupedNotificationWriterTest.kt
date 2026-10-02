package io.tolgee.notifications

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.service.notification.activity.GroupedNotificationWriter
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class GroupedNotificationWriterTest : AbstractSpringTest() {
  @Autowired
  private lateinit var writer: GroupedNotificationWriter

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var testData: NotificationRecipientsTestData

  @BeforeEach
  fun setup() {
    testData = NotificationRecipientsTestData()
    testDataService.saveTestData(testData.root)
  }

  private fun write(
    ids: List<Long>,
    digest: Boolean = true,
  ) = executeInNewTransaction(platformTransactionManager) {
    writer.write(
      userId = testData.reviewerAll.id,
      projectId = testData.project.id,
      originatingUserId = testData.author.id,
      type = NotificationType.STRINGS_TRANSLATED,
      entityIds = ids,
      digestEnabled = digest,
    )
  }

  @Test
  fun `two writes share one unseen row and count distinct entities`() {
    val first = write(listOf(1L, 2L))
    val second = write(listOf(2L, 3L))
    second.id.assert.isEqualTo(first.id)
    writer.entityCount(first.id).assert.isEqualTo(3)
  }

  @Test
  fun `count stops at the cap`() {
    val n = write((1L..90L).toList())
    write((50L..150L).toList())
    writer.entityCount(n.id).assert.isEqualTo(100)
  }

  @Test
  fun `seen row gets a new row on the next write`() {
    val first = write(listOf(1L))
    jdbcTemplate.update("update notification set seen = true where id = ?", first.id)
    val second = write(listOf(2L))
    second.id.assert.isNotEqualTo(first.id)
    writer.entityCount(second.id).assert.isEqualTo(1)
  }

  @Test
  fun `email pending follows the digest flag and is never cleared by a write`() {
    val n = write(listOf(1L), digest = false)
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isFalse()
    write(listOf(2L), digest = true)
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isTrue()
    write(listOf(3L), digest = false)
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isTrue()
  }
}
