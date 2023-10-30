package io.tolgee.ee.component.cdn

import io.tolgee.constants.Message
import io.tolgee.dtos.cdn.AzureCdnConfigDto
import io.tolgee.dtos.cdn.CdnStorageDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.cdn.AzureCdnConfig
import io.tolgee.model.cdn.CdnStorage
import io.tolgee.model.cdn.CdnStorageType
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class AzureCdnConfigProcessor : CdnConfigProcessor<AzureCdnConfigDto?, AzureCdnConfig> {
  override fun getItemFromDto(dto: CdnStorageDto): AzureCdnConfigDto? {
    return dto.azureCdnConfig
  }

  override fun clearParentEntity(storageEntity: CdnStorage, em: EntityManager) {
    storageEntity.azureCdnConfig?.let { em.remove(it) }
    storageEntity.azureCdnConfig = null
  }

  override val type: CdnStorageType
    get() = CdnStorageType.AZURE

  override fun configDtoToEntity(
    dto: CdnStorageDto,
    storageEntity: CdnStorage,
    em: EntityManager
  ): AzureCdnConfig {
    val azureDto = dto.azureCdnConfig ?: throw BadRequestException(Message.AZURE_CONFIG_REQUIRED)
    val entity = AzureCdnConfig(storageEntity)
    entity.connectionString = azureDto.connectionString
    entity.containerName = azureDto.containerName
    storageEntity.azureCdnConfig = entity
    em.persist(entity)
    return entity
  }
}
