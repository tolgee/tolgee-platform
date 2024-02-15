package io.tolgee.ee.component.contentDelivery

import io.tolgee.constants.Message
import io.tolgee.dtos.contentDelivery.AzureContentStorageConfigDto
import io.tolgee.dtos.contentDelivery.ContentStorageRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.contentDelivery.AzureContentStorageConfig
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.model.contentDelivery.ContentStorageType
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class AzureContentStorageConfigProcessor : ContentStorageConfigProcessor<AzureContentStorageConfig> {
  override fun getItemFromDto(dto: ContentStorageRequest): AzureContentStorageConfigDto? {
    return dto.azureContentStorageConfig
  }

  override fun clearParentEntity(
    storageEntity: ContentStorage,
    em: EntityManager,
  ) {
    storageEntity.azureContentStorageConfig?.let { em.remove(it) }
    storageEntity.azureContentStorageConfig = null
  }

  override val type: ContentStorageType
    get() = ContentStorageType.AZURE

  override fun configDtoToEntity(
    dto: ContentStorageRequest,
    storageEntity: ContentStorage,
    em: EntityManager,
  ): AzureContentStorageConfig {
    val azureDto = dto.azureContentStorageConfig ?: throw BadRequestException(Message.AZURE_CONFIG_REQUIRED)
    val entity = AzureContentStorageConfig(storageEntity)
    validateSecrets(azureDto)
    entity.connectionString =
      azureDto.connectionString
    entity.containerName = azureDto.containerName
    storageEntity.azureContentStorageConfig = entity
    em.persist(entity)
    return entity
  }

  private fun validateSecrets(azureDto: AzureContentStorageConfigDto) {
    if (azureDto.connectionString.isNullOrBlank()) {
      throw BadRequestException(Message.AZURE_CONNECTION_STRING_REQUIRED)
    }
  }

  override fun fillDtoSecrets(
    storageEntity: ContentStorage,
    dto: ContentStorageRequest,
  ) {
    val azureDto = dto.azureContentStorageConfig ?: return
    val entity = storageEntity.azureContentStorageConfig ?: return
    if (azureDto.connectionString.isNullOrBlank()) {
      azureDto.connectionString = entity.connectionString
    }
  }
}
