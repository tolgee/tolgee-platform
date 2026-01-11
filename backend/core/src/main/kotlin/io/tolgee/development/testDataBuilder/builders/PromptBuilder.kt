package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.Prompt

class PromptBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<Prompt, ProjectBuilder> {
  override var self: Prompt = Prompt(project = projectBuilder.self)
}
