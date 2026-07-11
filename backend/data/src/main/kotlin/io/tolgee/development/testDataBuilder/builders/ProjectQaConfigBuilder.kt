package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.qa.ProjectQaConfig

class ProjectQaConfigBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<ProjectQaConfig, ProjectQaConfigBuilder> {
  override var self: ProjectQaConfig = ProjectQaConfig(project = projectBuilder.self)
}
