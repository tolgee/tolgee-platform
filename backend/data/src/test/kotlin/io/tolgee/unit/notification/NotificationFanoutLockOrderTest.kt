package io.tolgee.unit.notification

import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.activity.RecipientNotification
import io.tolgee.service.notification.activity.inLockOrder
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class NotificationFanoutLockOrderTest {
  @Test
  fun `member-major recipients are ordered by type, then by ascending user id`() {
    val memberMajor =
      listOf(
        recipient(3, NotificationType.SOURCE_CHANGED),
        recipient(3, NotificationType.KEYS_ADDED),
        recipient(1, NotificationType.SOURCE_CHANGED),
        recipient(1, NotificationType.KEYS_ADDED),
        recipient(2, NotificationType.KEYS_ADDED),
      )

    inLockOrder(memberMajor).map { it.type to it.userId }.assert.containsExactly(
      NotificationType.KEYS_ADDED to 1L,
      NotificationType.KEYS_ADDED to 2L,
      NotificationType.KEYS_ADDED to 3L,
      NotificationType.SOURCE_CHANGED to 1L,
      NotificationType.SOURCE_CHANGED to 3L,
    )
  }

  private fun recipient(
    userId: Long,
    type: NotificationType,
  ) = RecipientNotification(userId, type, emptyList())
}
