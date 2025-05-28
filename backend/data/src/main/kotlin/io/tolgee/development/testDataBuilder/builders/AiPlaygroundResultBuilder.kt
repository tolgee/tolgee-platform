package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.AiPlaygroundResult

class AiPlaygroundResultBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<AiPlaygroundResult, ProjectBuilder> {
  override var self: AiPlaygroundResult =
    AiPlaygroundResult(
      project = projectBuilder.self,
    )
}
