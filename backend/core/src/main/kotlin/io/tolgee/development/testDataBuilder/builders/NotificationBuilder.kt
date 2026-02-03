package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.notifications.Notification

class NotificationBuilder(
  val userAccountBuilder: UserAccountBuilder,
) : EntityDataBuilder<Notification, NotificationBuilder> {
  override var self: Notification =
    Notification().apply {
      this.user = userAccountBuilder.self
    }
}
