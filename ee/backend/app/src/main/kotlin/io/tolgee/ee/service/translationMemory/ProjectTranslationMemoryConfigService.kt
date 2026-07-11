package io.tolgee.ee.service.translationMemory

import io.tolgee.constants.Message
import io.tolgee.ee.data.translationMemory.AssignSharedTranslationMemoryRequest
import io.tolgee.ee.data.translationMemory.UpdateProjectTranslationMemoryAssignmentRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryProject
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.repository.translationMemory.TranslationMemoryRepository
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectTranslationMemoryConfigService(
  private val translationMemoryRepository: TranslationMemoryRepository,
  private val translationMemoryProjectRepository: TranslationMemoryProjectRepository,
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

  @Transactional
  fun unassignSharedTm(
    projectId: Long,
    translationMemoryId: Long,
  ) {
    val assignment = getAssignment(projectId, translationMemoryId)

    // Cannot unassign project from its own PROJECT-type TM
    if (assignment.translationMemory.type == TranslationMemoryType.PROJECT) {
      throw BadRequestException(Message.CANNOT_UNASSIGN_PROJECT_FROM_OWN_TRANSLATION_MEMORY)
    }

    translationMemoryProjectRepository.delete(assignment)
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
