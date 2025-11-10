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
) : Logging {
  @Transactional
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#project.id")
  fun hardDeleteProject(project: Project) {
    traceLogMeasureTime("deleteProject") {
      try {
        projectHolder.project
      } catch (e: ProjectNotSelectedException) {
        projectHolder.project = ProjectDto.fromEntity(project)
      }

      traceLogMeasureTime("deleteProject: unassign glossaries") {
        glossaryCleanupService.unassignFromAllProjects(project)
      }

      traceLogMeasureTime("deleteProject: delete project import settings") {
        importSettingsService.deleteAllByProject(project.id)
      }

      importService.getAllByProject(project.id).forEach {
        importService.hardDeleteImport(it)
      }

      // otherwise we cannot delete the languages
      project.baseLanguage = null
      projectRepository.saveAndFlush(project)

      traceLogMeasureTime("deleteProject: delete api keys") {
        apiKeyService.deleteAllByProject(project.id)
      }

      traceLogMeasureTime("deleteProject: delete permissions") {
        permissionService.deleteAllByProject(project.id)
      }

      traceLogMeasureTime("deleteProject: delete screenshots") {
        screenshotService.deleteAllByProject(project.id)
      }

      mtServiceConfigService.deleteAllByProjectId(project.id)

      promptService.deleteAllByProjectId(project.id)

      aiPlaygroundResultService.deleteResultsByProject(project.id)

      labelService.deleteLabelsByProjectId(project.id)

      traceLogMeasureTime("deleteProject: delete languages") {
        languageService.deleteAllByProject(project.id)
      }

      traceLogMeasureTime("deleteProject: delete keys") {
        keyService.deleteAllByProject(project.id)
      }

      avatarService.unlinkAvatarFiles(project)
      batchJobService.deleteAllByProjectId(project.id)
      bigMetaService.deleteAllByProjectId(project.id)
      projectRepository.delete(project)
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
  @Async
  fun onProjectSoftDeleted(event: OnProjectSoftDeleted) {
    self.hardDeleteProject(event.project)
  }
}
