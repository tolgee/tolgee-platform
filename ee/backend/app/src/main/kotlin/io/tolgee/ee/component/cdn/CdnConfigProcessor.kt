package io.tolgee.ee.component.cdn

import io.tolgee.dtos.cdn.CdnStorageRequest
import io.tolgee.model.cdn.CdnStorage
import io.tolgee.model.cdn.CdnStorageType
import io.tolgee.model.cdn.StorageConfig
import javax.persistence.EntityManager

interface CdnConfigProcessor<EntityType> {
  fun getItemFromDto(dto: CdnStorageRequest): StorageConfig?
  fun configDtoToEntity(dto: CdnStorageRequest, storageEntity: CdnStorage, em: EntityManager): EntityType
  fun clearParentEntity(storageEntity: CdnStorage, em: EntityManager)
  fun fillDtoSecrets(storageEntity: CdnStorage, dto: CdnStorageRequest)

  val type: CdnStorageType
}
