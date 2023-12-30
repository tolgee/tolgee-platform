package io.tolgee.ee.component.contentDelivery

import io.tolgee.dtos.contentDelivery.ContentStorageRequest
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.model.contentDelivery.ContentStorageType
import io.tolgee.model.contentDelivery.StorageConfig
import jakarta.persistence.EntityManager

interface ContentStorageConfigProcessor<EntityType> {
  fun getItemFromDto(dto: ContentStorageRequest): StorageConfig?

  fun configDtoToEntity(
    dto: ContentStorageRequest,
    storageEntity: ContentStorage,
    em: EntityManager,
  ): EntityType

  fun clearParentEntity(
    storageEntity: ContentStorage,
    em: EntityManager,
  )

  fun fillDtoSecrets(
    storageEntity: ContentStorage,
    dto: ContentStorageRequest,
  )

  val type: ContentStorageType
}
