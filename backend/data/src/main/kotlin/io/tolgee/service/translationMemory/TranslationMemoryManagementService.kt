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

  /**
   * Bulk-removes the shared TM assignments matching [translationMemoryIds] from [projectId].
   * Used by the base-language change flow to detach mismatched shared TMs atomically inside
   * the project-edit transaction. No feature gate — the caller (`PROJECT_EDIT` on the project)
   * is the authority; this exists so users on plans without the TM feature can still rescue
   * a project that carries leftover shared-TM assignments.
   *
   * Project-type TMs are silently filtered out — those cannot be detached from their own
   * project. Unknown TM ids are simply not matched (no-op).
   */
  @Transactional
  fun unassignSharedTmsByProject(
    projectId: Long,
    translationMemoryIds: Collection<Long>,
  ) {
    if (translationMemoryIds.isEmpty()) return
    val assignments =
      translationMemoryProjectRepository
        .findByProjectId(projectId)
        .filter {
          it.translationMemory.id in translationMemoryIds &&
            it.translationMemory.type != TranslationMemoryType.PROJECT
        }
    if (assignments.isEmpty()) return
    translationMemoryProjectRepository.deleteAll(assignments)
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
   * Retrieves the IDs of translation memories (TMs) that are readable for a given project.
   * If the "TRANSLATION_MEMORY" feature is enabled for the organization, both project-specific
   * and shared translation memories are considered. Otherwise, only project-specific TMs are returned.
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
