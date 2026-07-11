package io.tolgee.hateoas.ee.contentStorage

import io.tolgee.constants.Feature
import io.tolgee.exceptions.NotImplementedInOss
import io.tolgee.model.contentDelivery.ContentStorage
import org.springframework.stereotype.Component

@Component
class ContentStorageModelAssemblerOssImpl : ContentStorageModelAssembler {
  override fun toModel(entity: ContentStorage): ContentStorageModel {
    throw NotImplementedInOss(Feature.PROJECT_LEVEL_CONTENT_STORAGES)
  }
}
