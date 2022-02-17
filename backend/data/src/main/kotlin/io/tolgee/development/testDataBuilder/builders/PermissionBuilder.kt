package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.Permission

class PermissionBuilder(
  val projectBuilder: ProjectBuilder
) : EntityDataBuilder<Permission, PermissionBuilder> {
  override var self: Permission = Permission().apply {
    project = projectBuilder.self
  }
}
