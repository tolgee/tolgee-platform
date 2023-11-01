package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.cdn.CdnExporter

class CdnExporterBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<CdnExporter, CdnExporterBuilder> {
  override var self: CdnExporter = CdnExporter(projectBuilder.self)
}
