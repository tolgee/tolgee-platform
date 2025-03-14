package io.tolgee.hateoas.contentDelivery

import io.tolgee.api.v2.controllers.contentDelivery.ContentDeliveryConfigController
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.hateoas.ee.contentStorage.ContentStorageModelAssembler
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ContentDeliveryConfigModelAssembler(
  private val contentStorageModelAssembler: ContentStorageModelAssembler,
  private val tolgeeProperties: TolgeeProperties,
) : RepresentationModelAssemblerSupport<ContentDeliveryConfig, ContentDeliveryConfigModel>(
    ContentDeliveryConfigController::class.java,
    ContentDeliveryConfigModel::class.java,
  ) {
  override fun toModel(entity: ContentDeliveryConfig): ContentDeliveryConfigModel {
    return ContentDeliveryConfigModel(
      id = entity.id,
      name = entity.name,
      slug = entity.slug,
      pruneBeforePublish = entity.pruneBeforePublish,
      storage = entity.contentStorage?.let { contentStorageModelAssembler.toModel(it) },
      publicUrl = getPublicUrl(entity),
      autoPublish = entity.automationActions.isNotEmpty(),
      lastPublished = entity.lastPublished?.time,
      lastPublishedFiles = entity.lastPublishedFiles ?: listOf(),
      escapeHtml = entity.escapeHtml,
    ).also {
      it.copyPropsFrom(entity)
    }
  }

  private fun getPublicUrl(entity: ContentDeliveryConfig): String? {
    if (entity.contentStorage != null) {
      return entity.contentStorage?.publicUrlPrefix?.let { it.removeSuffix("/") + "/" + entity.slug }
    }
    return tolgeeProperties.contentDelivery.publicUrlPrefix?.let { it.removeSuffix("/") + "/" + entity.slug }
  }
}
