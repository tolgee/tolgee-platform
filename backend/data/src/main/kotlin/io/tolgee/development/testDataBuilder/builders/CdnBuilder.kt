package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.cdn.Cdn

class CdnBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<Cdn, CdnBuilder> {
  override var self: Cdn = Cdn(projectBuilder.self)
}
