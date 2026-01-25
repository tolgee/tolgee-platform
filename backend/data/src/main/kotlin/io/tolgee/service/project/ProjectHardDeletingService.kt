package io.tolgee.service.project

import io.tolgee.batch.BatchJobService
import io.tolgee.constants.Caches
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.events.OnProjectSoftDeleted
import io.tolgee.model.Project
import io.tolgee.repository.ProjectRepository
import io.tolgee.security.ProjectHolder
import io.tolgee.security.ProjectNotSelectedException
import io.tolgee.service.AiPlaygroundResultService
import io.tolgee.service.AvatarService
import io.tolgee.service.GlossaryCleanupService
import io.tolgee.service.PromptService
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.dataImport.ImportSettingsService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.label.LabelService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.MtServiceConfigService
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.PermissionService
import io.tolgee.util.Logging
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.CacheEvict
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Suppress("SelfReferenceConstructorParameter")
@Service
class ProjectHardDeletingService(
  private val projectHolder: ProjectHolder,
  private val importService: ImportService,
  private val apiKeyService: ApiKeyService,
  private val permissionService: PermissionService,
  private val projectRepository: ProjectRepository,
  private val languageService: LanguageService,
  private val keyService: KeyService,
  private val screenshotService: ScreenshotService,
  private val avatarService: AvatarService,
  private val batchJobService: BatchJobService,
  private val bigMetaService: BigMetaService,
  private val mtServiceConfigService: MtServiceConfigService,
  @Lazy
  private val self: ProjectHardDeletingService,
  private val aiPlaygroundResultService: AiPlaygroundResultService,
  @Qualifier("promptServiceEeImpl") private val promptService: PromptService,
  private val importSettingsService: ImportSettingsService,
  private val glossaryCleanupService: GlossaryCleanupService,
  private val labelService: LabelService,
  private val entityManager: EntityManager,
) : Logging {
  @Transactional
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#project.id")
  fun hardDeleteProject(project: Project) {
    traceLogMeasureTime("deleteProject") {
      // Store the project ID upfront to use after clearing the persistence context
      val projectId = project.id

      try {
        projectHolder.project
      } catch (e: ProjectNotSelectedException) {
        projectHolder.project = ProjectDto.fromEntity(project)
      }

      traceLogMeasureTime("deleteProject: unassign glossaries") {
        glossaryCleanupService.unassignFromAllProjects(project)
      }

      traceLogMeasureTime("deleteProject: delete project import settings") {
        importSettingsService.deleteAllByProject(projectId)
      }

      importService.getAllByProject(projectId).forEach {
        importService.hardDeleteImport(it)
      }

      // otherwise we cannot delete the languages
      project.baseLanguage = null
      projectRepository.saveAndFlush(project)

      traceLogMeasureTime("deleteProject: delete api keys") {
        apiKeyService.deleteAllByProject(projectId)
      }

      traceLogMeasureTime("deleteProject: delete permissions") {
        permissionService.deleteAllByProject(projectId)
      }

      traceLogMeasureTime("deleteProject: delete screenshots") {
        screenshotService.deleteAllByProject(projectId)
      }

      mtServiceConfigService.deleteAllByProjectId(projectId)

      promptService.deleteAllByProjectId(projectId)

      aiPlaygroundResultService.deleteResultsByProject(projectId)

      labelService.deleteLabelsByProjectId(projectId)

      traceLogMeasureTime("deleteProject: delete languages") {
        languageService.deleteAllByProject(projectId)
      }

      traceLogMeasureTime("deleteProject: delete keys") {
        keyService.deleteAllByProject(projectId)
      }

      avatarService.unlinkAvatarFiles(project)

      // Flush and clear before batch job deletion to prevent TransientObjectException
      // The batch job service uses entity-based deletion which can cause issues
      // when entities in the persistence context reference batch jobs
      entityManager.flush()
      entityManager.clear()

      batchJobService.deleteAllByProjectId(projectId)

      // Flush and clear after batch job deletion to prevent TransientObjectException
      // when BigMetaService tries to query entities that might reference batch jobs
      entityManager.flush()
      entityManager.clear()

      bigMetaService.deleteAllByProjectId(projectId)

      // Flush and clear the persistence context to ensure deletions are synchronized
      // and to prevent Hibernate 6.6's CHECK_ON_FLUSH from seeing stale relationships
      entityManager.flush()
      entityManager.clear()

      // Delete entities with orphanRemoval=true that would normally be cascade-deleted
      // We need to do this explicitly because we're using a bulk JPQL delete for the project
      // Order matters due to foreign key constraints

      // Delete Automation children first (they reference Automation and ContentDeliveryConfig)
      entityManager
        .createQuery("DELETE FROM AutomationTrigger t WHERE t.automation.project.id = :projectId")
        .setParameter("projectId", projectId)
        .executeUpdate()
      entityManager
        .createQuery("DELETE FROM AutomationAction a WHERE a.automation.project.id = :projectId")
        .setParameter("projectId", projectId)
        .executeUpdate()

      // Now delete Automation and other project-level entities
      entityManager
        .createQuery("DELETE FROM Automation a WHERE a.project.id = :projectId")
        .setParameter("projectId", projectId)
        .executeUpdate()
      entityManager
        .createQuery("DELETE FROM AutoTranslationConfig a WHERE a.project.id = :projectId")
        .setParameter("projectId", projectId)
        .executeUpdate()
      entityManager
        .createQuery("DELETE FROM ContentDeliveryConfig c WHERE c.project.id = :projectId")
        .setParameter("projectId", projectId)
        .executeUpdate()

      // Delete ContentStorage children first
      entityManager
        .createQuery("DELETE FROM AzureContentStorageConfig a WHERE a.contentStorage.project.id = :projectId")
        .setParameter("projectId", projectId)
        .executeUpdate()
      entityManager
        .createQuery("DELETE FROM S3ContentStorageConfig s WHERE s.contentStorage.project.id = :projectId")
        .setParameter("projectId", projectId)
        .executeUpdate()

      entityManager
        .createQuery("DELETE FROM ContentStorage c WHERE c.project.id = :projectId")
        .setParameter("projectId", projectId)
        .executeUpdate()
      entityManager
        .createQuery("DELETE FROM WebhookConfig w WHERE w.project.id = :projectId")
        .setParameter("projectId", projectId)
        .executeUpdate()
      entityManager
        .createQuery("DELETE FROM SlackConfig s WHERE s.project.id = :projectId")
        .setParameter("projectId", projectId)
        .executeUpdate()

      // Use bulk delete to bypass Hibernate's orphan removal checking
      // which can cause TransientObjectException with Hibernate 6.6's stricter validation
      entityManager
        .createQuery("DELETE FROM Project p WHERE p.id = :projectId")
        .setParameter("projectId", projectId)
        .executeUpdate()
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
  @Async
  fun onProjectSoftDeleted(event: OnProjectSoftDeleted) {
    self.hardDeleteProject(event.project)
  }
}
