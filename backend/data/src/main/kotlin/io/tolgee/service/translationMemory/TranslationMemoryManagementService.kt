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
    // Priority 0 puts the project's own TM at the top; later-assigned shared TMs stack under
    // it via max+1 in the shared-assignment service.
    assignment.priority = 0
    translationMemoryProjectRepository.save(assignment)

    return tm
  }

  @Transactional
  fun getOrCreateProjectTm(project: Project): TranslationMemory {
    return getProjectTm(project.id) ?: createProjectTm(project)
  }

  /**
   * Re-fetches the project inside the current transaction so lazy associations
   * (e.g. `baseLanguage`, `organizationOwner`) can be resolved. Use this variant from
   * async event handlers where the Project reference may have been loaded in a prior
   * transaction and is now detached.
   */
  @Transactional
  fun getOrCreateProjectTm(projectId: Long): TranslationMemory {
    return getProjectTm(projectId) ?: createProjectTm(projectService.get(projectId))
  }

  /**
   * Lazy provisioning hook: if the organization has the TRANSLATION_MEMORY feature but the
   * project has no project-TM config row yet, create one. Short-circuits on the common case
   * (TM already exists) with a single indexed query. No data backfill involved — entries are
   * virtual.
   */
  @Transactional
  fun ensureProjectTmIfFeatureEnabled(
    projectId: Long,
    organizationId: Long,
  ) {
    if (getProjectTm(projectId) != null) return
    if (!enabledFeaturesProvider.isFeatureEnabled(organizationId, Feature.TRANSLATION_MEMORY)) return
    createProjectTm(projectService.get(projectId))
  }

  /**
   * Updates the project TM's [TranslationMemory.sourceLanguageTag] to the project's current base
   * language tag. Called from [io.tolgee.service.project.ProjectService.editProject] when the base
   * language changes. No entries to rebuild — the content is virtual.
   */
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

  /**
   * Returns all SHARED TM assignments for a project (every assignment whose TM is not the
   * project's own PROJECT TM). Used when validating base-language changes — the caller must
   * reject the change if any assigned shared TM's `sourceLanguageTag` would disagree with
   * the new project base.
   */
  fun getSharedTmAssignmentsForProject(projectId: Long): List<TranslationMemoryProject> {
    return translationMemoryProjectRepository
      .findByProjectId(projectId)
      .filter { it.translationMemory.type != TranslationMemoryType.PROJECT }
  }

  /**
   * Keeps the project TM's [TranslationMemory.name] in sync with the owning project's name.
   * The project TM is created with `name = project.name`; without this, a rename leaves the TM
   * showing the stale name in the org-level TM list.
   */
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

  /**
   * Toggles the reviewed-only flag on the project's own TM. Project TMs don't support the
   * full [io.tolgee.ee.data.translationMemory.UpdateSharedTranslationMemoryRequest] (name,
   * assignments, and base language are derived from the project) so this narrow setter
   * exists so project admins can flip this single setting via the project TM settings
   * dialog without touching the shared-TM endpoint.
   */
  @Transactional
  fun setProjectTmWriteOnlyReviewed(
    projectId: Long,
    writeOnlyReviewed: Boolean,
  ): TranslationMemory? {
    val tm = getProjectTm(projectId) ?: return null
    tm.writeOnlyReviewed = writeOnlyReviewed
    return translationMemoryRepository.save(tm)
  }

  fun getReadableTmIds(projectId: Long): List<Long> {
    return translationMemoryProjectRepository
      .findByProjectIdAndReadAccessTrue(projectId)
      .map { it.translationMemory.id }
  }

  /**
   * Plan-aware variant used by the suggestion path.
   * - If `Feature.TRANSLATION_MEMORY` is enabled → returns all readable TM IDs (project + shared).
   * - Otherwise (free plan) → returns only the project's own PROJECT-type TM ID.
   *
   * Free plan users get suggestions from their own project TM but do not see shared TM matches.
   */
  fun getReadableTmIdsForSuggestions(
    projectId: Long,
    organizationId: Long,
  ): List<Long> {
    val readable = translationMemoryProjectRepository.findByProjectIdAndReadAccessTrue(projectId)
    if (enabledFeaturesProvider.isFeatureEnabled(organizationId, Feature.TRANSLATION_MEMORY)) {
      return readable.map { it.translationMemory.id }
    }
    return readable
      .filter { it.translationMemory.type == TranslationMemoryType.PROJECT }
      .map { it.translationMemory.id }
  }

  fun getWritableTmAssignments(projectId: Long): List<TranslationMemoryProject> {
    return translationMemoryProjectRepository.findByProjectIdAndWriteAccessTrue(projectId)
  }

  /**
   * Cleans up TM data when a project is deleted.
   * Deletes the project's own TM (type=PROJECT) and removes assignments from shared TMs.
   */
  @Transactional
  fun deleteAllByProject(projectId: Long) {
    val assignments = translationMemoryProjectRepository.findByProjectId(projectId)
    val projectTms = assignments.filter { it.translationMemory.type == TranslationMemoryType.PROJECT }

    // Remove all assignments for this project
    translationMemoryProjectRepository.deleteByProjectId(projectId)

    // Delete project-type TMs entirely (entries cascade via DB FK)
    projectTms.forEach { translationMemoryRepository.delete(it.translationMemory) }
  }
}
