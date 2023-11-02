package io.tolgee.service.automations

import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.automations.AutomationDto
import io.tolgee.dtos.request.CdnExporterDto
import io.tolgee.dtos.request.DefaultCdnDto
import io.tolgee.dtos.request.automation.AutomationActionRequest
import io.tolgee.dtos.request.automation.AutomationRequest
import io.tolgee.dtos.request.automation.AutomationTriggerRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.automations.Automation
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationActionType
import io.tolgee.model.automations.AutomationTrigger
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.model.cdn.CdnExporter
import io.tolgee.repository.AutomationRepository
import io.tolgee.service.cdn.CdnExporterService
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class AutomationService(
  private val cacheManager: CacheManager,
  private val entityManager: EntityManager,
  private val applicationContext: ApplicationContext,
  private val automationRepository: AutomationRepository,
  private val cdnExporterService: CdnExporterService
) {
  @Cacheable(value = [Caches.AUTOMATIONS], key = "{#projectId, #automationTriggerType, #activityType}")
  @Transactional
  fun getProjectAutomations(
    projectId: Long,
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType? = null
  ): List<AutomationDto> {
    val automations = getAutomationWithFetchedData(projectId, automationTriggerType, activityType)
    return automations.map { AutomationDto.fromEntity(it) }
  }

  @Transactional
  fun save(automation: Automation): Automation {
    automation.triggers.forEach { entityManager.persist(it) }
    automation.actions.forEach { entityManager.persist(it) }
    entityManager.persist(automation)

    automation.triggers.forEach {
      getCache().evict(listOf(automation.project.id, it.type, it.activityType))
    }
    return automation
  }

  @Transactional
  fun delete(automation: Automation) {
    automation.triggers.forEach {
      getCache().evict(listOf(automation.project.id, it.type, it.activityType))
    }
  }

  @Transactional
  fun delete(id: Long) {
    automationRepository.deleteById(id)
  }

  private fun getCache(): Cache {
    return cacheManager.getCache(Caches.AUTOMATIONS)!!
  }

  @Transactional
  fun getAction(actionId: Long): AutomationAction {
    return entityManager.createQuery(
      """
      from AutomationAction aa 
      join fetch aa.automation
      where aa.id = :actionId
      """,
      AutomationAction::class.java
    )
      .setParameter("actionId", actionId)
      .singleResult ?: throw NotFoundException()
  }

  @Transactional
  fun update(projectId: Long, automationId: Long, dto: AutomationRequest): Automation {
    val automation = getAutomationWithFetchedData(automationId, projectId)
    dtoToEntity(automation, dto)
    deleteTriggersAndActions(automation)
    setNewTriggersAndActions(dto, automation)
    return save(automation)
  }

  private fun deleteTriggersAndActions(automation: Automation) {
    automation.actions.removeAll {
      entityManager.remove(it)
      true
    }
    automation.triggers.removeAll {
      entityManager.remove(it)
      true
    }
    automation.actions.clear()
    automation.triggers.clear()
  }

  @Transactional
  fun create(projectId: Long, dto: AutomationRequest, projectDefault: Boolean = false): Automation {
    val automation = Automation(entityManager.getReference(Project::class.java, projectId))
    dtoToEntity(automation, dto)
    setNewTriggersAndActions(dto, automation)
    automation.projectDefault = projectDefault
    return save(automation)
  }

  private fun dtoToEntity(
    automation: Automation,
    dto: AutomationRequest
  ) {
    automation.name = dto.name
  }

  @Transactional
  fun getProjectAutomations(projectId: Long, pageable: Pageable): Page<Automation> {
    val automations = automationRepository.findAllInProject(projectId, pageable)
    automationRepository.fetchTriggers(automations.content)
    return automations
  }

  @Transactional
  fun get(id: Long): Automation {
    return find(id) ?: throw NotFoundException()
  }

  @Transactional
  fun find(id: Long): Automation? {
    return automationRepository.find(id)
  }

  @Transactional
  fun delete(projectId: Long, automationId: Long) {
    val deletedCunt = automationRepository.deleteByIdAndProjectId(automationId, projectId)
    if (deletedCunt == 0L) {
      throw NotFoundException()
    }
  }

  @Transactional
  fun get(projectId: Long, automationId: Long): Automation {
    return automationRepository.findByIdAndProjectId(automationId, projectId)
      ?: throw NotFoundException()
  }

  private fun getAutomationWithFetchedData(
    projectId: Long,
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType? = null
  ): MutableList<Automation> {
    val automations = getAutomationsWithTriggerOfType(projectId, automationTriggerType, activityType)

    return entityManager.createQuery(
      """from Automation a join fetch a.actions where a in :automations""",
      Automation::class.java
    ).setParameter("automations", automations).resultList
  }

  private fun getAutomationWithFetchedData(
    automationId: Long,
    projectId: Long
  ): Automation {
    val withActions = entityManager.createQuery(
      """from Automation a 
        left join fetch a.actions aa
        left join fetch aa.cdnExporter ce
        left join fetch ce.cdnStorage
        where a.id = :automationId 
        and a.project.id = :projectId""",
      Automation::class.java
    )
      .setParameter("automationId", automationId)
      .setParameter("projectId", projectId)
      .singleResult

    val withTriggers = entityManager.createQuery(
      """from Automation a 
        join fetch a.triggers 
        where a = :automation""",
      Automation::class.java
    )
      .setParameter("automation", withActions)
      .singleResult

    return withTriggers
  }

  private fun getAutomationsWithTriggerOfType(
    projectId: Long,
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType?
  ): MutableList<Automation>? = entityManager.createQuery(
    """
            from Automation a join fetch a.triggers
            where a.id in (
                select a2.id from Automation a2 
                join a2.triggers at 
                  where a2.project.id = :projectId
                   and at.type = :automationTriggerType
                   and (at.activityType = :activityType or (:activityType is null and at.activityType is null))
            )
    """.trimIndent(),
    Automation::class.java
  )
    .setParameter("projectId", projectId)
    .setParameter("automationTriggerType", automationTriggerType)
    .setParameter("activityType", activityType)
    .resultList

  private fun setNewTriggersAndActions(
    dto: AutomationRequest,
    automation: Automation
  ) {
    val triggers = dto.triggers.map {
      AutomationTrigger(automation).apply {
        this.activityType = it.activityType
        this.type = it.type
        this.debounceDurationInMs = it.debounceDurationInMs
      }
    }.toMutableList()

    val actions = dto.actions.map { actionRequest ->
      AutomationAction(automation).apply {
        this.type = actionRequest.type
        val processor = applicationContext.getBean(actionRequest.type.processor.java)
        processor.fillEntity(actionRequest, this)
      }
    }.toMutableList()

    automation.actions.addAll(actions)
    automation.triggers.addAll(triggers)
  }

  @Transactional
  fun createProjectDefaultCdn(projectId: Long, dto: DefaultCdnDto): Automation {
    if (projectDefaultExists(projectId)) {
      throw BadRequestException(Message.PROJECT_ALREADY_HAS_DEFAULT_CDN_CONFIGURED)
    }
    val exporter = getDefaultExporter(dto, projectId)

    return this.create(
      projectId = projectId,
      dto = AutomationRequest(
        name = "Default CDN Automation",
        triggers = listOf(
          AutomationTriggerRequest(
            type = AutomationTriggerType.TRANSLATION_DATA_MODIFICATION,
            activityType = null,
            debounceDurationInMs = 5000
          )
        ),
        actions = listOf(
          AutomationActionRequest(
            type = AutomationActionType.CDN_PUBLISH,
            cdnExporterId = exporter.id
          )
        ),
      ),
      projectDefault = true
    )
  }

  private fun getDefaultExporter(
      dto: DefaultCdnDto,
      projectId: Long
  ): CdnExporter {
    val exporterDto = CdnExporterDto()
    exporterDto.copyPropsFrom(dto)
    exporterDto.name = "Default Project CDN"
    val exporter = cdnExporterService.create(projectId, exporterDto)
    return exporter
  }

  private fun projectDefaultExists(projectId: Long): Boolean =
    automationRepository.existsByProjectIdAndProjectDefault(projectId)

  fun getDefaultProjectAutomation(projectId: Long): Automation? {
    val id = automationRepository.getProjectDefaultId(projectId) ?: return null
    return getAutomationWithFetchedData(id, projectId)
  }
}
