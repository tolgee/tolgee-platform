package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount

class PermissionBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<Permission, PermissionBuilder> {
  override var self: Permission =
    Permission(user = projectBuilder.onlyUser ?: UserAccount()).apply {
      project = projectBuilder.self
    }
}
