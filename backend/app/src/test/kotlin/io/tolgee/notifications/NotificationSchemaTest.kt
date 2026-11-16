package io.tolgee.notifications

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.NotificationsTestData
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate

class NotificationSchemaTest : AbstractSpringTest() {
  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Test
  fun `grouped index covers exactly the grouped types`() {
    val definition =
      jdbcTemplate.queryForObject(
        "select indexdef from pg_indexes where indexname = 'notification_grouped_unseen_idx'",
        String::class.java,
      )!!
    NotificationType.entries.forEach { type ->
      if (type.grouped) {
        definition.assert.contains("'${type.name}'")
      } else {
        definition.assert.doesNotContain("'${type.name}'")
      }
    }
  }

  @Test
  fun `second unseen grouped row for the same user, project and type is rejected`() {
    val testData = NotificationsTestData()
    testDataService.saveTestData(testData.root)
    notificationRepository.saveAndFlush(groupedNotification(testData, seen = false))
    assertThatThrownBy {
      notificationRepository.saveAndFlush(groupedNotification(testData, seen = false))
    }.isInstanceOf(DataIntegrityViolationException::class.java)
  }

  @Test
  fun `seen grouped rows do not block a new unseen row`() {
    val testData = NotificationsTestData()
    testDataService.saveTestData(testData.root)
    notificationRepository.saveAndFlush(groupedNotification(testData, seen = true))
    notificationRepository.saveAndFlush(groupedNotification(testData, seen = false))
    notificationRepository
      .findAll()
      .filter { it.type == NotificationType.KEYS_ADDED }
      .assert
      .hasSize(2)
  }

  private fun groupedNotification(
    testData: NotificationsTestData,
    seen: Boolean,
  ) = Notification().apply {
    user = testData.user
    project = testData.project
    type = NotificationType.KEYS_ADDED
    this.seen = seen
    emailPending = true
  }
}
