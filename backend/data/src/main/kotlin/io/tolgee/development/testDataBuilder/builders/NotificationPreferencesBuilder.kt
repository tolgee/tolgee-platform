package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.notifications.NotificationPreferences

class NotificationPreferencesBuilder(
  val userAccountBuilder: UserAccountBuilder
) : EntityDataBuilder<NotificationPreferences, NotificationPreferencesBuilder> {
  override var self: NotificationPreferences =
    NotificationPreferences(userAccountBuilder.self, null, emptyArray())
}
