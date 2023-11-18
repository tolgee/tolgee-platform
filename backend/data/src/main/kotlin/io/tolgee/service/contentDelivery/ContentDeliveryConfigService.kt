package io.tolgee.service.contentDelivery

import io.tolgee.component.ContentStorageProvider
import io.tolgee.component.contentDelivery.ContentDeliveryUploader
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.request.ContentDeliveryConfigRequest
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.repository.contentDelivery.ContentDeliveryConfigRepository
import io.tolgee.service.automations.AutomationService
import io.tolgee.service.project.ProjectService
import io.tolgee.util.SlugGenerator
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional
import kotlin.random.Random

@Service
class ContentDeliveryConfigService(
  private val contentDeliveryConfigRepository: ContentDeliveryConfigRepository,
  private val slugGenerator: SlugGenerator,
  private val entityManager: EntityManager,
  private val contentStorageProvider: ContentStorageProvider,
  private val projectService: ProjectService,
  private val automationService: AutomationService,
  @Lazy
  private val contentDeliveryUploader: ContentDeliveryUploader,
  private val enabledFeaturesProvider: EnabledFeaturesProvider
) {
  @Transactional
  fun create(projectId: Long, dto: ContentDeliveryConfigRequest): ContentDeliveryConfig {
    val project = entityManager.getReference(Project::class.java, projectId)
    checkMultipleConfigsFeature(project)
    val contentDeliveryConfig = ContentDeliveryConfig(project)
    contentDeliveryConfig.name = dto.name
    contentDeliveryConfig.contentStorage = getStorage(projectId, dto.contentStorageId)
    contentDeliveryConfig.copyPropsFrom(dto)
    contentDeliveryConfig.slug = generateSlug(projectId)
    contentDeliveryConfigRepository.save(contentDeliveryConfig)
    if (dto.autoPublish) {
      automationService.createForContentDelivery(contentDeliveryConfig)
      contentDeliveryUploader.upload(contentDeliveryConfig.id)
    }
    return contentDeliveryConfig
  }

  private fun checkMultipleConfigsFeature(project: Project) {
    if (contentDeliveryConfigRepository.countByProject(project) > 0) {
      enabledFeaturesProvider.checkFeatureEnabled(
        project.organizationOwner.id,
        Feature.MULTIPLE_CONTENT_DELIVERY_CONFIGS
      )
    }
  }

  fun generateSlug(projectId: Long): String {
    val projectDto = projectService.getDto(projectId)

    return slugGenerator.generate(random32byteHexString(), 3, 50) {
      contentDeliveryConfigRepository.isSlugUnique(projectDto.id, it)
    }
  }

  fun random32byteHexString(): String {
    return (1..32).joinToString("") { Random.nextInt(0, 16).toString(16) }
  }

  private fun getStorage(projectId: Long, contentStorageId: Long?): ContentStorage? {
    contentStorageId ?: return null
    return contentStorageProvider.getStorage(projectId, contentStorageId)
  }

  fun get(id: Long) = find(id) ?: throw NotFoundException()

  fun find(id: Long) = contentDeliveryConfigRepository.findById(id).orElse(null)

  @Transactional
  fun update(projectId: Long, id: Long, dto: ContentDeliveryConfigRequest): ContentDeliveryConfig {
    val exporter = get(projectId, id)
    exporter.contentStorage = getStorage(projectId, dto.contentStorageId)
    exporter.name = dto.name
    exporter.copyPropsFrom(dto)
    handleUpdateAutoPublish(dto, exporter)
    return contentDeliveryConfigRepository.save(exporter)
  }

  private fun handleUpdateAutoPublish(dto: ContentDeliveryConfigRequest, exporter: ContentDeliveryConfig) {
    if (dto.autoPublish && exporter.automationActions.isEmpty()) {
      automationService.createForContentDelivery(exporter)
    }
    if (!dto.autoPublish && exporter.automationActions.isNotEmpty()) {
      automationService.removeForContentDelivery(exporter)
    }
  }

  fun delete(projectId: Long, id: Long) {
    val config = get(projectId, id)
    config.automationActions.map { it.automation }.forEach {
      automationService.delete(it)
    }
    contentDeliveryConfigRepository.deleteById(config.id)
  }

  fun getAllInProject(projectId: Long, pageable: Pageable): Page<ContentDeliveryConfig> {
    return contentDeliveryConfigRepository.findAllByProjectId(projectId, pageable)
  }

  fun get(projectId: Long, contentDeliveryConfigId: Long): ContentDeliveryConfig {
    return contentDeliveryConfigRepository.getByProjectIdAndId(projectId, contentDeliveryConfigId)
  }
}
