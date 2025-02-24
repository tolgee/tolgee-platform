package io.tolgee.testing

import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.model.notifications.Notification
import io.tolgee.repository.notification.NotificationRepository
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.stereotype.Component

@Component
class NotificationTestUtil(
  private val notificationRepository: NotificationRepository,
  private val emailTestUtil: EmailTestUtil,
) {
  fun init() = emailTestUtil.initMocks()

  fun newestInAppNotification(): Notification = notificationRepository.findAll(Sort.by(DESC, "id")).first()

  fun assertNoInAppNotifications() {
    if (notificationRepository.findAll().isNotEmpty()) {
      throw AssertionError("There should be no in-app notifications.")
    }
  }

  fun newestEmailNotification(): String = emailTestUtil.messageContents.last()

  fun assertNoEmailNotifications() {
    if (emailTestUtil.messageContents.isNotEmpty()) {
      throw AssertionError("There should be no email notifications.")
    }
  }
}
