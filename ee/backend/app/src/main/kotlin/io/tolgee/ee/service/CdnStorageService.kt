package io.tolgee.ee.service

import io.tolgee.component.cdn.CdnFileStorageProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.cdn.CdnStorageDto
import io.tolgee.ee.component.cdn.CdnConfigProcessor
import io.tolgee.ee.data.StorageTestResult
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ExceptionWithMessage
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.cdn.CdnStorage
import io.tolgee.model.cdn.CdnStorageType
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
  private val cdnFileStorageProvider: CdnFileStorageProvider,
  private val cdnConfigProcessors: List<CdnConfigProcessor<*, *>>
) {
  @Transactional
  fun create(projectId: Long, dto: CdnStorageDto): CdnStorage {
    validateStorage(dto)
    val project = entityManager.getReference(Project::class.java, projectId)
    val storage = CdnStorage(project, dto.name)
    storage.project = project
    dtoToEntity(dto, storage)
    cdnStorageRepository.save(storage)
    return storage
  }

  fun get(id: Long) = find(id) ?: throw NotFoundException()

  fun find(id: Long) = cdnStorageRepository.findById(id).orElse(null)

  @Transactional
  fun update(projectId: Long, id: Long, dto: CdnStorageDto): CdnStorage {
    validateStorage(dto)
    val cdnStorage = get(id)
    clearOther(cdnStorage)
    dtoToEntity(dto, cdnStorage)
    return cdnStorageRepository.save(cdnStorage)
  }

  private fun clearOther(cdnStorage: CdnStorage) {
    CdnStorageType.entries.toTypedArray().forEach { getProcessor(it).clearParentEntity(cdnStorage, entityManager) }
  }

  @Transactional
  fun delete(projectId: Long, id: Long) {
    val storage = get(projectId, id)
    cdnConfigProcessors.forEach { it.clearParentEntity(storage, entityManager) }
    cdnStorageRepository.delete(storage)
  }

  fun getAllInProject(projectId: Long, pageable: Pageable): Page<CdnStorage> {
    return cdnStorageRepository.findAllByProjectId(projectId, pageable)
  }

  fun get(projectId: Long, cdnId: Long): CdnStorage {
    return cdnStorageRepository.getByProjectIdAndId(projectId, cdnId)
  }

  fun testStorage(dto: CdnStorageDto): StorageTestResult {
    val config: StorageConfig = getNonNullConfig(dto)
    try {
      val storage = cdnFileStorageProvider.getStorage(config)
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
    @Suppress("UNCHECKED_CAST")
    if (!result.pass) throw BadRequestException(
      Message.CDN_STORAGE_CONFIG_INVALID,
      listOf(result.message, result.params) as List<Serializable?>?
    )
  }

  private fun getNonNullConfig(dto: CdnStorageDto): StorageConfig {
    validateDto(dto)
    val config: StorageConfig = when {
      dto.azureCdnConfig != null -> dto.azureCdnConfig!!
      dto.s3CdnConfig != null -> dto.s3CdnConfig!!
      else -> throw BadRequestException(Message.CDN_STORAGE_CONFIG_REQUIRED)
    }
    return config
  }

  fun getStorageType(dto: CdnStorageDto): CdnStorageType = getNonNullConfig(dto).cdnStorageType

  private fun validateDto(dto: CdnStorageDto) {
    val isSingleConfig = (dto.azureCdnConfig != null) xor (dto.s3CdnConfig != null)
    if (!isSingleConfig) throw BadRequestException(Message.CDN_STORAGE_CONFIG_REQUIRED)
  }

  private fun dtoToEntity(dto: CdnStorageDto, entity: CdnStorage): Any {
    entity.name = dto.name
    entity.publicUrlPrefix = dto.publicUrlPrefix
    return getProcessorForDto(dto).configDtoToEntity(dto, entity, entityManager)!!
  }

  private fun getProcessorForDto(dto: CdnStorageDto): CdnConfigProcessor<*, *> {
    return getProcessor(getStorageType(dto))
  }

  private val processorCache = mutableMapOf<CdnStorageType, CdnConfigProcessor<*, *>>()
  fun getProcessor(type: CdnStorageType): CdnConfigProcessor<*, *> {
    return processorCache.computeIfAbsent(type) {
      cdnConfigProcessors.find { it.type == type }
        ?: throw IllegalStateException("Cannot find processor for type $type")
    }
  }
}
