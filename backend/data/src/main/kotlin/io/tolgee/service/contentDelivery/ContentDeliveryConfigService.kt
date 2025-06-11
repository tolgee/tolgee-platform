package io.tolgee.service.contentDelivery

import io.tolgee.component.contentDelivery.ContentDeliveryUploader
import io.tolgee.component.contentStorageProvider.ContentStorageProvider
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.dtos.request.ContentDeliveryConfigRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.repository.contentDelivery.ContentDeliveryConfigRepository
import io.tolgee.service.automations.AutomationService
import io.tolgee.service.project.ProjectService
import io.tolgee.util.SlugGenerator
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import kotlin.random.Random

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
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
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @Transactional
  fun create(
    projectId: Long,
    dto: ContentDeliveryConfigRequest,
  ): ContentDeliveryConfig {
    val project = entityManager.getReference(Project::class.java, projectId)
    checkMultipleConfigsFeature(project)
    val config = ContentDeliveryConfig(project)
    config.name = dto.name
    config.contentStorage = getStorage(projectId, dto.contentStorageId)
    config.copyPropsFrom(dto)
    setSlugForCreation(config, dto)
    config.pruneBeforePublish = dto.pruneBeforePublish
    config.escapeHtml = dto.escapeHtml
    contentDeliveryConfigRepository.save(config)
    if (dto.autoPublish) {
      automationService.createForContentDelivery(config)
      contentDeliveryUploader.upload(config.id)
    }
    return config
  }

  private fun setSlugForCreation(
    config: ContentDeliveryConfig,
    dto: ContentDeliveryConfigRequest,
  ) {
    val desiredSlug = dto.slug
    if (desiredSlug == null) {
      config.slug = generateSlug()
      config.customSlug = false
      return
    }

    if (dto.contentStorageId == null) {
      throw BadRequestException(Message.CUSTOM_SLUG_IS_ONLY_APPLICABLE_FOR_CUSTOM_STORAGE)
    }

    validateSlug(desiredSlug)

    config.slug = desiredSlug
    config.customSlug = true

    return
  }

  private fun validateSlug(slug: String) {
    val regex = "^[a-z0-9]+(?:-[a-z0-9]+)*$".toRegex()
    val matches = regex.matches(slug)
    if (!matches) {
      throw BadRequestException(Message.INVALID_SLUG_FORMAT)
    }
  }

  private fun checkMultipleConfigsFeature(
    project: Project,
    maxCurrentAllowed: Int = 0,
  ) {
    if (contentDeliveryConfigRepository.countByProject(project) > maxCurrentAllowed) {
      enabledFeaturesProvider.checkFeatureEnabled(
        project.organizationOwner.id,
        Feature.MULTIPLE_CONTENT_DELIVERY_CONFIGS,
      )
    }
  }

  fun generateSlug(): String {
    return slugGenerator.generate(random32byteHexString(), 3, 50) {
      contentDeliveryConfigRepository.isSlugUnique(it)
    }
  }

  fun random32byteHexString(): String {
    return (1..32).joinToString("") { Random.nextInt(0, 16).toString(16) }
  }

  private fun getStorage(
    projectId: Long,
    contentStorageId: Long?,
  ): ContentStorage? {
    contentStorageId ?: return null
    return contentStorageProvider.getStorage(projectId, contentStorageId)
  }

  fun get(id: Long) = find(id) ?: throw NotFoundException()

  fun find(id: Long) = contentDeliveryConfigRepository.findById(id).orElse(null)

  @Transactional
  fun update(
    projectId: Long,
    id: Long,
    dto: ContentDeliveryConfigRequest,
  ): ContentDeliveryConfig {
    checkMultipleConfigsFeature(projectService.get(projectId), maxCurrentAllowed = 1)
    val config = get(projectId, id)
    handleUpdateSlug(config, dto)
    config.contentStorage = getStorage(projectId, dto.contentStorageId)
    config.name = dto.name
    config.pruneBeforePublish = dto.pruneBeforePublish
    config.copyPropsFrom(dto)
    handleUpdateAutoPublish(dto, config)
    return save(config)
  }

  private fun handleUpdateSlug(
    config: ContentDeliveryConfig,
    dto: ContentDeliveryConfigRequest,
  ) {
    val desiredSlug = dto.slug
    val wasCustomStorage = config.contentStorage != null
    val nowCustomStorage = dto.contentStorageId != null

    if (!wasCustomStorage && !nowCustomStorage && desiredSlug == null) {
      return
    }

    if (desiredSlug == null) {
      config.slug = generateSlug()
      config.customSlug = false
      return
    }

    val customStorageRemoved = wasCustomStorage && !nowCustomStorage
    val illegalKeepOfCustomSlug = customStorageRemoved && config.customSlug
    val illegalUseOfCustomSlug = desiredSlug != config.slug && !nowCustomStorage

    if (illegalUseOfCustomSlug || illegalKeepOfCustomSlug) {
      throw BadRequestException(Message.CUSTOM_SLUG_IS_ONLY_APPLICABLE_FOR_CUSTOM_STORAGE)
    }

    validateSlug(desiredSlug)

    config.slug = desiredSlug
    config.customSlug = true
  }

  private fun handleUpdateAutoPublish(
    dto: ContentDeliveryConfigRequest,
    exporter: ContentDeliveryConfig,
  ) {
    if (dto.autoPublish && exporter.automationActions.isEmpty()) {
      automationService.createForContentDelivery(exporter)
    }
    if (!dto.autoPublish && exporter.automationActions.isNotEmpty()) {
      automationService.removeForContentDelivery(exporter)
    }
  }

  fun delete(
    projectId: Long,
    id: Long,
  ) {
    val config = get(projectId, id)
    config.automationActions.map { it.automation }.forEach {
      automationService.delete(it)
    }
    contentDeliveryConfigRepository.deleteById(config.id)
  }

  fun getAllInProject(
    projectId: Long,
    pageable: Pageable,
  ): Page<ContentDeliveryConfig> {
    return contentDeliveryConfigRepository.findAllByProjectId(projectId, pageable)
  }

  fun get(
    projectId: Long,
    contentDeliveryConfigId: Long,
  ): ContentDeliveryConfig {
    return contentDeliveryConfigRepository.getByProjectIdAndId(projectId, contentDeliveryConfigId)
  }

  fun save(config: ContentDeliveryConfig): ContentDeliveryConfig {
    return contentDeliveryConfigRepository.save(config)
  }
}
