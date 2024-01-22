package io.tolgee.ee.service

import io.tolgee.component.contentDelivery.ContentDeliveryFileStorageProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.contentDelivery.ContentStorageRequest
import io.tolgee.ee.component.contentDelivery.ContentStorageConfigProcessor
import io.tolgee.ee.data.StorageTestResult
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ExceptionWithMessage
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.model.contentDelivery.ContentStorageType
import io.tolgee.model.contentDelivery.StorageConfig
import io.tolgee.repository.contentDelivery.ContentStorageRepository
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.io.Serializable

@Service
class ContentStorageService(
  private val contentStorageRepository: ContentStorageRepository,
  private val entityManager: EntityManager,
  private val contentDeliveryFileStorageProvider: ContentDeliveryFileStorageProvider,
  private val contentStorageConfigProcessors: List<ContentStorageConfigProcessor<*>>,
) {
  @Transactional
  fun create(
    projectId: Long,
    dto: ContentStorageRequest,
  ): ContentStorage {
    validateStorage(dto)
    val project = entityManager.getReference(Project::class.java, projectId)
    val storage = ContentStorage(project, dto.name)
    storage.project = project
    dtoToEntity(dto, storage)
    contentStorageRepository.save(storage)
    return storage
  }

  fun get(id: Long) = find(id) ?: throw NotFoundException()

  fun find(id: Long) = contentStorageRepository.findById(id).orElse(null)

  @Transactional
  fun update(
    projectId: Long,
    id: Long,
    dto: ContentStorageRequest,
  ): ContentStorage {
    val contentStorage = get(id)
    getProcessor(getStorageType(dto)).fillDtoSecrets(contentStorage, dto)
    validateStorage(dto)
    clearOther(contentStorage)
    entityManager.flush()
    dtoToEntity(dto, contentStorage)
    entityManager.persist(contentStorage)
    return contentStorage
  }

  private fun clearOther(contentStorage: ContentStorage) {
    ContentStorageType.values().forEach {
      getProcessor(it).clearParentEntity(contentStorage, entityManager)
    }
  }

  @Transactional
  fun delete(
    projectId: Long,
    id: Long,
  ) {
    val storage = get(projectId, id)
    contentStorageConfigProcessors.forEach { it.clearParentEntity(storage, entityManager) }
    if (contentStorageRepository.isStorageInUse(storage)) {
      throw BadRequestException(Message.CONTENT_STORAGE_IS_IN_USE)
    }
    contentStorageRepository.delete(storage)
  }

  fun getAllInProject(
    projectId: Long,
    pageable: Pageable,
  ): Page<ContentStorage> {
    return contentStorageRepository.findAllByProjectId(projectId, pageable)
  }

  fun get(
    projectId: Long,
    contentDeliveryConfigId: Long,
  ): ContentStorage {
    return contentStorageRepository.getByProjectIdAndId(projectId, contentDeliveryConfigId)
  }

  fun testStorage(
    dto: ContentStorageRequest,
    id: Long? = null,
  ): StorageTestResult {
    val config: StorageConfig = getNonNullConfig(dto)
    if (id != null) {
      val existing = get(id)
      getProcessor(config.contentStorageType).fillDtoSecrets(existing, dto)
    }
    try {
      val storage = contentDeliveryFileStorageProvider.getStorage(config)
      storage.test()
    } catch (e: Exception) {
      if (e is ExceptionWithMessage) {
        return StorageTestResult(false, e.tolgeeMessage, e.params)
      }
      return StorageTestResult(false, Message.CONTENT_STORAGE_TEST_FAILED)
    }
    return StorageTestResult(true)
  }

  private fun validateStorage(dto: ContentStorageRequest) {
    val result = testStorage(dto)
    @Suppress("UNCHECKED_CAST")
    if (!result.success) {
      throw BadRequestException(
        Message.CONTENT_STORAGE_CONFIG_INVALID,
        listOf(result.message, result.params) as List<Serializable?>?,
      )
    }
  }

  private fun getNonNullConfig(dto: ContentStorageRequest): StorageConfig {
    validateDto(dto)
    return contentStorageConfigProcessors.firstNotNullOfOrNull {
      it.getItemFromDto(dto)
    } ?: throw BadRequestException(Message.CONTENT_STORAGE_CONFIG_REQUIRED)
  }

  fun getStorageType(dto: ContentStorageRequest): ContentStorageType = getNonNullConfig(dto).contentStorageType

  private fun validateDto(dto: ContentStorageRequest) {
    val isSingleConfig =
      contentStorageConfigProcessors.count {
        it.getItemFromDto(dto) != null
      } == 1
    if (!isSingleConfig) throw BadRequestException(Message.CONTENT_STORAGE_CONFIG_REQUIRED)
  }

  private fun dtoToEntity(
    dto: ContentStorageRequest,
    entity: ContentStorage,
  ): Any {
    entity.name = dto.name
    entity.publicUrlPrefix = dto.publicUrlPrefix
    return getProcessorForDto(dto).configDtoToEntity(dto, entity, entityManager)!!
  }

  private fun getProcessorForDto(dto: ContentStorageRequest): ContentStorageConfigProcessor<*> {
    return getProcessor(getStorageType(dto))
  }

  private val processorCache = mutableMapOf<ContentStorageType, ContentStorageConfigProcessor<*>>()

  fun getProcessor(type: ContentStorageType): ContentStorageConfigProcessor<*> {
    return processorCache.computeIfAbsent(type) {
      contentStorageConfigProcessors.find { it.type == type }
        ?: throw IllegalStateException("Cannot find processor for type $type")
    }
  }
}
