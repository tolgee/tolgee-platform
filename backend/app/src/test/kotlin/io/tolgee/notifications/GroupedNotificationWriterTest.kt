package io.tolgee.notifications

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.model.UserAccount
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.service.notification.activity.GroupedNotificationWrite
import io.tolgee.service.notification.activity.GroupedNotificationWriter
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  private fun write(
    ids: List<Long>,
    emailEnabled: Boolean = true,
  ) = executeInNewTransaction(platformTransactionManager) { writeInCurrentTransaction(ids, emailEnabled) }

  private fun writeInCurrentTransaction(
    ids: List<Long>,
    emailEnabled: Boolean = true,
  ) = writer.write(
    GroupedNotificationWrite(
      userId = testData.reviewerAll.id,
      projectId = testData.project.id,
      originatingUserId = testData.author.id,
      type = NotificationType.STRINGS_TRANSLATED,
      entityIds = ids,
      emailEnabled = emailEnabled,
    ),
  )

  @Test
  fun `two writes share one unseen row and count distinct entities`() {
    val first = write(listOf(1L, 2L))
    val second = write(listOf(2L, 3L))
    second.id.assert.isEqualTo(first.id)
    writer.entityCount(first.id).assert.isEqualTo(3)
  }

  @Test
  fun `write racing an uncommitted insert of the same item joins it instead of failing`() {
    val firstInserted = CountDownLatch(1)
    val secondStarted = CountDownLatch(1)
    val first =
      CompletableFuture.supplyAsync {
        executeInNewTransaction(platformTransactionManager) {
          val written = writeInCurrentTransaction(listOf(1L, 2L))
          firstInserted.countDown()
          secondStarted.await(10, TimeUnit.SECONDS)
          Thread.sleep(300)
          written.id
        }
      }
    val second =
      CompletableFuture.supplyAsync {
        firstInserted.await(10, TimeUnit.SECONDS)
        secondStarted.countDown()
        write(listOf(3L)).id
      }

    val firstId = first.get(20, TimeUnit.SECONDS)
    second.get(20, TimeUnit.SECONDS).assert.isEqualTo(firstId)
    writer.entityCount(firstId).assert.isEqualTo(3)
    jdbcTemplate
      .queryForObject(
        "select count(*) from notification where user_id = ? and type = 'STRINGS_TRANSLATED'",
        Int::class.java,
        testData.reviewerAll.id,
      ).assert
      .isEqualTo(1)
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
  fun `email pending follows emailEnabled and is never cleared by a write`() {
    val n = write(listOf(1L), emailEnabled = false)
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isFalse()
    write(listOf(2L), emailEnabled = true)
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isTrue()
    write(listOf(3L), emailEnabled = false)
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isTrue()
  }

  @Test
  fun `revision ids keep the latest 200`() {
    val n =
      executeInNewTransaction(platformTransactionManager) {
        writer.write(
          GroupedNotificationWrite(
            userId = testData.reviewerAll.id,
            projectId = testData.project.id,
            originatingUserId = testData.author.id,
            type = NotificationType.STRINGS_TRANSLATED,
            entityIds = listOf(1L),
            emailEnabled = true,
            revisionIds = (1L..150L).toList(),
          ),
        )
      }
    executeInNewTransaction(platformTransactionManager) {
      writer.write(
        GroupedNotificationWrite(
          userId = testData.reviewerAll.id,
          projectId = testData.project.id,
          originatingUserId = testData.author.id,
          type = NotificationType.STRINGS_TRANSLATED,
          entityIds = listOf(2L),
          emailEnabled = true,
          revisionIds = (101L..250L).toList(),
        ),
      )
    }
    val ids =
      jdbcTemplate.queryForList(
        "select unnest(activity_revision_ids) from notification where id = ?",
        Long::class.java,
        n.id,
      )
    ids.assert.hasSize(200)
    ids.first().assert.isEqualTo(51L)
    ids.last().assert.isEqualTo(250L)
  }

  @Test
  fun `write does not clear the callers persistence context`() {
    executeInNewTransaction(platformTransactionManager) {
      val user = entityManager.find(UserAccount::class.java, testData.reviewerAll.id)
      writer.write(
        GroupedNotificationWrite(
          userId = testData.reviewerAll.id,
          projectId = testData.project.id,
          originatingUserId = testData.author.id,
          type = NotificationType.STRINGS_TRANSLATED,
          entityIds = listOf(1L),
          emailEnabled = true,
        ),
      )
      entityManager.contains(user).assert.isTrue()
    }
  }
}
