package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.UserSession
import java.util.UUID

class UserSessionBuilder(
  val userAccountBuilder: UserAccountBuilder,
) : EntityDataBuilder<UserSession, UserSessionBuilder> {
  override var self: UserSession =
    UserSession().apply {
      deviceId = UUID.randomUUID().toString()
    }
}
