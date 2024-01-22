package io.tolgee.hateoas.ee.contentStorage

import io.tolgee.model.contentDelivery.ContentStorage

interface IContentStorageModelAssembler {
  fun toModel(entity: ContentStorage): ContentStorageModel
}
