package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.key.Tag

class TagBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<Tag, TagBuilder> {
  override var self: Tag =
    Tag().apply {
      this.project = projectBuilder.self
    }
}
