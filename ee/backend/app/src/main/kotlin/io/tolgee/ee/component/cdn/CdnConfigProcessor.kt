package io.tolgee.ee.component.cdn

import io.tolgee.dtos.cdn.CdnStorageDto
import io.tolgee.model.cdn.CdnStorage
import io.tolgee.model.cdn.CdnStorageType
import javax.persistence.EntityManager

interface CdnConfigProcessor<ConfigDtoType, EntityType> {
  fun getItemFromDto(dto: CdnStorageDto): ConfigDtoType?
  fun configDtoToEntity(dto: CdnStorageDto, storageEntity: CdnStorage, em: EntityManager): EntityType
  fun clearParentEntity(storageEntity: CdnStorage, em: EntityManager)

  val type: CdnStorageType
}
