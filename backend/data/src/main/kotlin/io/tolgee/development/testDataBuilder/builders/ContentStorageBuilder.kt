package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.contentDelivery.ContentStorage

class ContentStorageBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<ContentStorage, ContentStorageBuilder> {
  override var self: ContentStorage = ContentStorage(projectBuilder.self, "Azure")
}
