package io.tolgee.ee.service.translationMemory

import io.tolgee.constants.Message
import io.tolgee.ee.data.translationMemory.AssignSharedTranslationMemoryRequest
import io.tolgee.ee.data.translationMemory.UpdateProjectTranslationMemoryAssignmentRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.model.translationMemory.TranslationMemoryProject
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.repository.translationMemory.TranslationMemoryRepository
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectTranslationMemoryConfigService(
  private val translationMemoryRepository: TranslationMemoryRepository,
  private val translationMemoryProjectRepository: TranslationMemoryProjectRepository,
  private val translationMemoryEntryRepository: TranslationMemoryEntryRepository,
  private val translationMemoryManagementService: TranslationMemoryManagementService,
  private val projectService: ProjectService,
) {
  fun getAssignments(projectId: Long): List<TranslationMemoryProject> {
    return translationMemoryProjectRepository.findByProjectId(projectId)
  }

  fun getAssignment(
    projectId: Long,
    translationMemoryId: Long,
  ): TranslationMemoryProject {
    return translationMemoryProjectRepository
      .findByTranslationMemoryIdAndProjectId(translationMemoryId, projectId)
      ?: throw NotFoundException(Message.TRANSLATION_MEMORY_PROJECT_ASSIGNMENT_NOT_FOUND)
  }

  @Transactional
  fun assignSharedTm(
    projectId: Long,
    translationMemoryId: Long,
    dto: AssignSharedTranslationMemoryRequest,
  ): TranslationMemoryProject {
    val project = projectService.get(projectId)
    val tm = getTmInSameOrganization(project.organizationOwner.id, translationMemoryId)

    if (tm.type != TranslationMemoryType.SHARED) {
      throw BadRequestException(Message.CANNOT_MODIFY_PROJECT_TRANSLATION_MEMORY)
    }

    requireMatchingBaseLanguage(project, tm)

    val existing =
      translationMemoryProjectRepository.findByTranslationMemoryIdAndProjectId(
        translationMemoryId,
        projectId,
      )
    if (existing != null) {
      throw BadRequestException(Message.TRANSLATION_MEMORY_ALREADY_ASSIGNED_TO_PROJECT)
    }

    val assignment = TranslationMemoryProject()
    assignment.translationMemory = tm
    assignment.project = project
    assignment.readAccess = dto.readAccess
    assignment.writeAccess = dto.writeAccess
    assignment.priority = dto.priority ?: nextAvailablePriority(projectId)
    assignment.penalty = dto.penalty
    return translationMemoryProjectRepository.save(assignment)
  }

  private fun nextAvailablePriority(projectId: Long): Int {
    return translationMemoryProjectRepository
      .findByProjectId(projectId)
      .maxOfOrNull { it.priority }
      ?.plus(1) ?: 0
  }

  /**
   * Unassigns a shared TM from a project.
   *
   * When [keepData] is `true`, entries currently accessible via the shared TM are first
   * snapshotted into the project's own PROJECT-type TM so the project retains translation
   * memory data after the disconnect. When `false` (default), the assignment is simply
   * removed; the shared TM and its entries remain intact for other projects.
   */
  @Transactional
  fun unassignSharedTm(
    projectId: Long,
    translationMemoryId: Long,
    keepData: Boolean = false,
  ) {
    val assignment = getAssignment(projectId, translationMemoryId)

    // Cannot unassign project from its own PROJECT-type TM
    if (assignment.translationMemory.type == TranslationMemoryType.PROJECT) {
      throw BadRequestException(Message.CANNOT_UNASSIGN_PROJECT_FROM_OWN_TRANSLATION_MEMORY)
    }

    if (keepData) {
      snapshotEntriesIntoProjectTm(projectId, assignment.translationMemory.id)
    }

    translationMemoryProjectRepository.delete(assignment)
  }

  /**
   * Copies all entries from the given shared TM into the project's own PROJECT-type TM.
   *
   * The snapshot drops the `translation` and `key` back-references — these belong to the
   * original source project and have no meaning in the destination project. Only
   * `sourceText`, `targetText`, and `targetLanguageTag` are carried over.
   *
   * Idempotent: any entry whose `(source, target, language)` triple already exists in the
   * destination project TM is skipped, so calling this twice does not duplicate data.
   *
   * Inserts flow through Hibernate batch (`hibernate.jdbc.batch_size` is set in
   * `application.yaml`) — a `saveAll` over N filtered snapshots issues round trips in
   * groups of the configured batch size instead of one statement per row.
   *
   * **Transactional contract:** private to this service and must be invoked from a method
   * annotated with `@Transactional` (currently `unassignSharedTm`). The method relies on the
   * outer transaction for atomicity — if any part of the snapshot fails, the entire
   * disconnect is rolled back and the original assignment remains intact.
   */
  private fun snapshotEntriesIntoProjectTm(
    projectId: Long,
    sharedTmId: Long,
  ) {
    val projectTm = translationMemoryManagementService.getOrCreateProjectTm(projectId)

    val sourceEntries = translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId)
    if (sourceEntries.isEmpty()) return

    val existingKeys: Set<Triple<String, String, String>> =
      translationMemoryEntryRepository
        .findDedupKeysByTranslationMemoryId(projectTm.id)
        .mapTo(HashSet()) { row ->
          Triple(row[0] as String, row[1] as String, row[2] as String)
        }

    val snapshots =
      sourceEntries
        .filter { source ->
          Triple(source.sourceText, source.targetText, source.targetLanguageTag) !in existingKeys
        }.map { source ->
          TranslationMemoryEntry().apply {
            translationMemory = projectTm
            sourceText = source.sourceText
            targetText = source.targetText
            targetLanguageTag = source.targetLanguageTag
          }
        }
    if (snapshots.isEmpty()) return
    translationMemoryEntryRepository.saveAll(snapshots)
  }

  @Transactional
  fun updateAssignment(
    projectId: Long,
    translationMemoryId: Long,
    dto: UpdateProjectTranslationMemoryAssignmentRequest,
  ): TranslationMemoryProject {
    val assignment = getAssignment(projectId, translationMemoryId)
    val isProjectTm = assignment.translationMemory.type == TranslationMemoryType.PROJECT
    if (isProjectTm && (!dto.readAccess || !dto.writeAccess)) {
      throw BadRequestException(Message.CANNOT_MODIFY_PROJECT_TRANSLATION_MEMORY)
    }
    assignment.readAccess = dto.readAccess
    assignment.writeAccess = dto.writeAccess
    dto.priority?.let { assignment.priority = it }
    assignment.penalty = dto.penalty
    return translationMemoryProjectRepository.save(assignment)
  }

  private fun getTmInSameOrganization(
    organizationId: Long,
    translationMemoryId: Long,
  ): TranslationMemory {
    return translationMemoryRepository.find(organizationId, translationMemoryId)
      ?: throw NotFoundException(Message.TRANSLATION_MEMORY_NOT_FOUND)
  }

  /**
   * A shared TM only makes sense when the assigned project writes/reads in the same source
   * language the TM was declared with — otherwise writes would pollute the TM with entries
   * whose `sourceText` isn't in its declared language. Projects with no base language yet
   * (fresh setup) are allowed through and will get the check the next time.
   */
  private fun requireMatchingBaseLanguage(
    project: Project,
    tm: TranslationMemory,
  ) {
    val projectBaseTag = project.baseLanguage?.tag ?: return
    if (projectBaseTag != tm.sourceLanguageTag) {
      throw BadRequestException(
        Message.TRANSLATION_MEMORY_BASE_LANGUAGE_MISMATCH,
        listOf(tm.name, tm.sourceLanguageTag, projectBaseTag),
      )
    }
  }
}
