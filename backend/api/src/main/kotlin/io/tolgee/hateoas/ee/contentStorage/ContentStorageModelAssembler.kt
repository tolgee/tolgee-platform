package io.tolgee.hateoas.ee.contentStorage

import io.tolgee.model.contentDelivery.ContentStorage

interface ContentStorageModelAssembler {
  fun toModel(entity: ContentStorage): ContentStorageModel
}
