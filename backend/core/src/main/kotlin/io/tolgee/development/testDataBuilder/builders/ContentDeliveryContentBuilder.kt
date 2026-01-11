package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.contentDelivery.ContentDeliveryConfig

class ContentDeliveryContentBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<ContentDeliveryConfig, ContentDeliveryContentBuilder> {
  override var self: ContentDeliveryConfig = ContentDeliveryConfig(projectBuilder.self)
}
