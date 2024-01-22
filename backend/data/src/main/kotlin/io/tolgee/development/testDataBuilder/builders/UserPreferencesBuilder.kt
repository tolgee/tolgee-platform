package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.UserPreferences

class UserPreferencesBuilder(
  val userAccountBuilder: UserAccountBuilder,
) : EntityDataBuilder<UserPreferences, UserPreferencesBuilder> {
  override var self: UserPreferences = UserPreferences(userAccount = userAccountBuilder.self)
  lateinit var defaultOrganizationBuilder: OrganizationBuilder
}
