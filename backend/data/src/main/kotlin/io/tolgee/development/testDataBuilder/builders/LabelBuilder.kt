package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.translation.Label

class LabelBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<Label, LabelBuilder> {
  override var self: Label =
    Label().apply {
      this.project = projectBuilder.self
    }
}
