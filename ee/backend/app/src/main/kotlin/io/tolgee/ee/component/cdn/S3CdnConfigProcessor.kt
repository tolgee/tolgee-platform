package io.tolgee.ee.component.cdn

import io.tolgee.constants.Message
import io.tolgee.dtos.cdn.CdnStorageRequest
import io.tolgee.dtos.cdn.S3CdnConfigDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.cdn.CdnStorage
import io.tolgee.model.cdn.CdnStorageType
import io.tolgee.model.cdn.S3CdnConfig
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class S3CdnConfigProcessor : CdnConfigProcessor<S3CdnConfig> {
  override fun getItemFromDto(dto: CdnStorageRequest): S3CdnConfigDto? {
    return dto.s3CdnConfig
  }

  override fun clearParentEntity(storageEntity: CdnStorage, em: EntityManager) {
    storageEntity.s3CdnConfig?.let { em.remove(it) }
    storageEntity.s3CdnConfig = null
  }

  override val type: CdnStorageType
    get() = CdnStorageType.S3

  override fun configDtoToEntity(dto: CdnStorageRequest, storageEntity: CdnStorage, em: EntityManager): S3CdnConfig {
    val s3dto = dto.s3CdnConfig ?: throw BadRequestException(Message.S3_CONFIG_REQUIRED)
    val entity = S3CdnConfig(storageEntity)
    entity.accessKey = s3dto.accessKey ?: throw BadRequestException(Message.S3_ACCESS_KEY_REQUIRED)
    entity.secretKey = s3dto.secretKey ?: throw BadRequestException(Message.S3_SECRET_KEY_REQUIRED)
    entity.bucketName = s3dto.bucketName
    entity.signingRegion = s3dto.signingRegion
    entity.endpoint = s3dto.endpoint
    storageEntity.s3CdnConfig = entity
    em.persist(entity)
    return entity
  }

  override fun fillDtoSecrets(storageEntity: CdnStorage, dto: CdnStorageRequest) {
    val s3dto = dto.s3CdnConfig ?: return
    val entity = storageEntity.s3CdnConfig ?: return
    s3dto.accessKey = entity.accessKey
    s3dto.secretKey = entity.secretKey
  }
}
