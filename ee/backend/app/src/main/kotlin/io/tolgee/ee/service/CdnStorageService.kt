package io.tolgee.ee.service

import io.tolgee.component.cdn.CdnFileStorageProvider
import io.tolgee.constants.Message
import io.tolgee.ee.data.AzureCdnConfigDto
import io.tolgee.ee.data.CdnStorageDto
import io.tolgee.ee.data.S3CdnConfigDto
import io.tolgee.ee.data.StorageTestResult
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ExceptionWithMessage
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.cdn.AzureCdnConfig
import io.tolgee.model.cdn.CdnStorage
import io.tolgee.model.cdn.CdnStorageType
import io.tolgee.model.cdn.S3CdnConfig
import io.tolgee.model.cdn.StorageConfig
import io.tolgee.repository.cdn.CdnStorageRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.io.Serializable
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class CdnStorageService(
  private val cdnStorageRepository: CdnStorageRepository,
  private val entityManager: EntityManager,
  private val cdnFileStorageProvider: CdnFileStorageProvider
) {
  @Transactional
  fun create(projectId: Long, dto: CdnStorageDto): CdnStorage {
    validateStorage(dto)
    val project = entityManager.getReference(Project::class.java, projectId)
    val storage = CdnStorage(project, getStorageType(dto))
    storage.project = project
    dtoToEntity(dto, storage)
    cdnStorageRepository.save(storage)
    return storage
  }

  fun get(id: Long) = find(id) ?: throw NotFoundException()

  fun find(id: Long) = cdnStorageRepository.findById(id).orElse(null)

  @Transactional
  fun update(id: Long, dto: CdnStorageDto): CdnStorage {
    validateStorage(dto)
    val cdnStorage = get(id)
    cdnStorage.s3CdnConfig?.let { entityManager.remove(it) }
    cdnStorage.azureCdnConfig?.let { entityManager.remove(it) }
    dtoToEntity(dto, cdnStorage)
    return cdnStorageRepository.save(cdnStorage)
  }

  fun delete(id: Long) {
    cdnStorageRepository.deleteById(id)
  }

  fun getAllInProject(projectId: Long, pageable: Pageable): Page<CdnStorage> {
    return cdnStorageRepository.findAllByProjectId(projectId, pageable)
  }

  fun get(projectId: Long, cdnId: Long): CdnStorage {
    return cdnStorageRepository.getByProjectIdAndId(projectId, cdnId)
  }

  fun testStorage(dto: CdnStorageDto): StorageTestResult {
    val config: StorageConfig = getNonNullConfig(dto)
    val storage = cdnFileStorageProvider.getStorage(config)
    try {
      storage.test()
    } catch (e: Exception) {
      if (e is ExceptionWithMessage) {
        return StorageTestResult(false, e.tolgeeMessage, e.params)
      }
      return StorageTestResult(false, Message.CDN_STORAGE_TEST_FAILED)
    }
    return StorageTestResult(true)
  }

  private fun validateStorage(dto: CdnStorageDto) {
    val result = testStorage(dto)
    if (!result.pass) throw BadRequestException(
      Message.CDN_STORAGE_CONFIG_INVALID,
      listOf(result.message, result.params) as List<Serializable?>?
    )
  }

  private fun getNonNullConfig(dto: CdnStorageDto): StorageConfig {
    validateDto(dto)
    val config: StorageConfig = when {
      dto.azureCdnConfig != null -> dto.azureCdnConfig
      dto.s3CdnConfig != null -> dto.s3CdnConfig
      else -> throw BadRequestException(Message.CDN_STORAGE_CONFIG_REQUIRED)
    }
    return config
  }

  fun getStorageType(dto: CdnStorageDto): CdnStorageType = getNonNullConfig(dto).cdnStorageType

  private fun validateDto(dto: CdnStorageDto) {
    val isSingleConfig = (dto.azureCdnConfig != null) xor (dto.s3CdnConfig != null)
    if (!isSingleConfig) throw BadRequestException(Message.CDN_STORAGE_CONFIG_REQUIRED)
  }

  private fun dtoToEntity(dto: CdnStorageDto, storage: CdnStorage) {
    when (storage.type) {
      CdnStorageType.AZURE -> {
        dto.azureCdnConfig ?: throw BadRequestException(Message.AZURE_CONFIG_REQUIRED)
        val azureCdnConfig = AzureCdnConfig(storage)
        dtoToEntity(dto.azureCdnConfig, azureCdnConfig)
      }

      CdnStorageType.S3 -> {
        dto.s3CdnConfig ?: throw BadRequestException(Message.S3_CONFIG_REQUIRED)
        val s3CdnConfig = S3CdnConfig(storage)
        dtoToEntity(dto.s3CdnConfig, s3CdnConfig)
      }
    }
  }

  private fun dtoToEntity(s3CdnConfig: S3CdnConfigDto, s3CdnConfig1: S3CdnConfig) {
    s3CdnConfig1.accessKey = s3CdnConfig.accessKey
    s3CdnConfig1.secretKey = s3CdnConfig.secretKey
    s3CdnConfig1.bucketName = s3CdnConfig.bucketName
    s3CdnConfig1.signingRegion = s3CdnConfig.signingRegion
    s3CdnConfig.endpoint = s3CdnConfig.endpoint
  }

  private fun dtoToEntity(dto: AzureCdnConfigDto, azureCdnConfig: AzureCdnConfig) {
    azureCdnConfig.connectionString = dto.connectionString
    azureCdnConfig.containerName = dto.containerName
  }
}
