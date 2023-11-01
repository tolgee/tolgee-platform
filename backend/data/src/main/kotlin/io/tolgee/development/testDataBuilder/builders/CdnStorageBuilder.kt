package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.cdn.CdnStorage

class CdnStorageBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<CdnStorage, CdnStorageBuilder> {
  override var self: CdnStorage = CdnStorage(projectBuilder.self, "Azure")
}
