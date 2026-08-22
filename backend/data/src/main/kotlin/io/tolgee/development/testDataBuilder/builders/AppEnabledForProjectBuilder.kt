package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.apps.AppEnabledForProject

class AppEnabledForProjectBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<AppEnabledForProject, AppEnabledForProjectBuilder> {
  override var self: AppEnabledForProject =
    AppEnabledForProject().apply {
      project = projectBuilder.self
    }
}
