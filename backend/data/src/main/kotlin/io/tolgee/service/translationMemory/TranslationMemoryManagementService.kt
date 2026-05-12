package io.tolgee.service.translationMemory

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.model.Project
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryProject
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.repository.translationMemory.TranslationMemoryRepository
import io.tolgee.service.project.ProjectService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TranslationMemoryManagementService(
  private val translationMemoryRepository: TranslationMemoryRepository,
  private val translationMemoryProjectRepository: TranslationMemoryProjectRepository,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  @Lazy private val projectService: ProjectService,
) {
  /**
   * Creates the PROJECT-type TM for [project] as a pure config row plus its self-assignment.
   * Project TMs store no entries — their content is computed virtually from the project's
   * translations at read time — so no backfill is performed.
   */
  @Transactional
  fun createProjectTm(project: Project): TranslationMemory {
    val tm =
      TranslationMemory(
        name = project.name,
        sourceLanguageTag = project.baseLanguage?.tag ?: "",
        type = TranslationMemoryType.PROJECT,
      )
    tm.organizationOwner = project.organizationOwner
    translationMemoryRepository.save(tm)

    val assignment = TranslationMemoryProject()
    assignment.translationMemory = tm
    assignment.project = project
    assignment.readAccess = true
    assignment.writeAccess = true
    // Priority 0 keeps the project's own TM on top; shared TMs stack under it via max+1.
    assignment.priority = 0
    translationMemoryProjectRepository.save(assignment)

    return tm
  }

  @Transactional
  fun updateProjectTmSourceLanguage(projectId: Long) {
    val tm = getProjectTm(projectId) ?: return
    val project = projectService.get(projectId)
    val newTag = project.baseLanguage?.tag ?: ""
    if (tm.sourceLanguageTag == newTag) return
    tm.sourceLanguageTag = newTag
    translationMemoryRepository.save(tm)
  }

  fun getProjectTm(projectId: Long): TranslationMemory? {
    return translationMemoryProjectRepository
      .findByProjectId(projectId)
      .firstOrNull { it.translationMemory.type == TranslationMemoryType.PROJECT }
      ?.translationMemory
  }

  fun getSharedTmAssignmentsForProject(projectId: Long): List<TranslationMemoryProject> {
    return translationMemoryProjectRepository
      .findByProjectId(projectId)
      .filter { it.translationMemory.type != TranslationMemoryType.PROJECT }
  }

  @Transactional
  fun renameProjectTm(
    projectId: Long,
    newName: String,
  ) {
    val tm = getProjectTm(projectId) ?: return
    if (tm.name == newName) return
    tm.name = newName
    translationMemoryRepository.save(tm)
  }

  @Transactional
  fun setProjectTmWriteOnlyReviewed(
    projectId: Long,
    writeOnlyReviewed: Boolean,
  ): TranslationMemory? {
    val tm = getProjectTm(projectId) ?: return null
    tm.writeOnlyReviewed = writeOnlyReviewed
    return translationMemoryRepository.save(tm)
  }

  /**
   * Free plan returns only the project's own PROJECT-type TM; paid plans return all readable
   * TM IDs (project + shared).
   */
  fun getReadableTmIdsForSuggestions(
    projectId: Long,
    organizationId: Long,
  ): List<Long> {
    val featureEnabled =
      enabledFeaturesProvider.isFeatureEnabled(organizationId, Feature.TRANSLATION_MEMORY)
    return translationMemoryProjectRepository.findReadableTmIdsByProjectId(
      projectId = projectId,
      type = if (featureEnabled) null else TranslationMemoryType.PROJECT,
    )
  }

  @Transactional
  fun deleteAllByProject(projectId: Long) {
    val assignments = translationMemoryProjectRepository.findByProjectId(projectId)
    val projectTms = assignments.filter { it.translationMemory.type == TranslationMemoryType.PROJECT }

    translationMemoryProjectRepository.deleteByProjectId(projectId)

    // Delete project-type TMs entirely (entries cascade via DB FK)
    projectTms.forEach { translationMemoryRepository.delete(it.translationMemory) }
  }
}
