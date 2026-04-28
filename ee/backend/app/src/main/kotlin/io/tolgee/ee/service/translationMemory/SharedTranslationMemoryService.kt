package io.tolgee.ee.service.translationMemory

import io.tolgee.constants.Message
import io.tolgee.ee.data.translationMemory.CreateSharedTranslationMemoryRequest
import io.tolgee.ee.data.translationMemory.ProjectAssignmentDto
import io.tolgee.ee.data.translationMemory.UpdateSharedTranslationMemoryRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryProject
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.model.translationMemory.TranslationMemoryWithStats
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.repository.translationMemory.TranslationMemoryRepository
import io.tolgee.service.project.ProjectService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SharedTranslationMemoryService(
  private val translationMemoryRepository: TranslationMemoryRepository,
  private val translationMemoryProjectRepository: TranslationMemoryProjectRepository,
  private val projectService: ProjectService,
) {
  fun findAllPaged(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<TranslationMemory> {
    return translationMemoryRepository.findByOrganizationIdPaged(
      organizationId = organizationId,
      pageable = pageable,
      search = search,
    )
  }

  fun findAllWithStatsPaged(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
    type: String? = null,
  ): Page<TranslationMemoryWithStats> {
    return translationMemoryRepository.findByOrganizationIdWithStatsPaged(
      organizationId = organizationId,
      pageable = pageable,
      search = search,
      type = type,
    )
  }

  fun find(
    organizationId: Long,
    translationMemoryId: Long,
  ): TranslationMemory? {
    return translationMemoryRepository.find(organizationId, translationMemoryId)
  }

  fun get(
    organizationId: Long,
    translationMemoryId: Long,
  ): TranslationMemory {
    return find(organizationId, translationMemoryId)
      ?: throw NotFoundException(Message.TRANSLATION_MEMORY_NOT_FOUND)
  }

  fun getShared(
    organizationId: Long,
    translationMemoryId: Long,
  ): TranslationMemory {
    val tm = get(organizationId, translationMemoryId)
    if (tm.type != TranslationMemoryType.SHARED) {
      throw BadRequestException(Message.CANNOT_MODIFY_PROJECT_TRANSLATION_MEMORY)
    }
    return tm
  }

  @Transactional
  fun create(
    organization: Organization,
    dto: CreateSharedTranslationMemoryRequest,
  ): TranslationMemory {
    val tm =
      TranslationMemory(
        name = dto.name,
        sourceLanguageTag = dto.sourceLanguageTag,
        type = TranslationMemoryType.SHARED,
        defaultPenalty = dto.defaultPenalty ?: 0,
        writeOnlyReviewed = dto.writeOnlyReviewed ?: false,
      )
    tm.organizationOwner = organization
    val saved = translationMemoryRepository.save(tm)
    resolveAndApplyAssignments(saved, organization.id, dto.assignedProjects, dto.assignedProjectIds)
    return saved
  }

  @Transactional
  fun update(
    organizationId: Long,
    translationMemoryId: Long,
    dto: UpdateSharedTranslationMemoryRequest,
  ): TranslationMemory {
    val tm = getShared(organizationId, translationMemoryId)
    if (dto.sourceLanguageTag != tm.sourceLanguageTag) {
      // Base-language invariant: a TM's sourceLanguageTag can only be changed while no
      // projects are assigned. Assignments are validated to match, so any change here
      // would leave existing ones in an illegal state.
      val hasAssignments =
        translationMemoryProjectRepository.findByTranslationMemoryId(tm.id).isNotEmpty()
      if (hasAssignments) {
        throw BadRequestException(Message.CANNOT_CHANGE_TM_BASE_LANGUAGE_WHILE_ASSIGNED)
      }
    }
    tm.name = dto.name
    tm.sourceLanguageTag = dto.sourceLanguageTag
    tm.defaultPenalty = dto.defaultPenalty ?: 0
    tm.writeOnlyReviewed = dto.writeOnlyReviewed ?: false
    resolveAndApplyAssignments(tm, organizationId, dto.assignedProjects, dto.assignedProjectIds)
    return translationMemoryRepository.save(tm)
  }

  /**
   * Narrow setter for the reviewed-only flag that works on both PROJECT and SHARED TMs.
   * Used by the org TM list so org maintainers can toggle the flag on any TM without going
   * through the full [update] path (which rejects PROJECT TMs via [getShared]).
   */
  @Transactional
  fun setWriteOnlyReviewed(
    organizationId: Long,
    translationMemoryId: Long,
    writeOnlyReviewed: Boolean,
  ): TranslationMemory {
    val tm = get(organizationId, translationMemoryId)
    tm.writeOnlyReviewed = writeOnlyReviewed
    return translationMemoryRepository.save(tm)
  }

  private fun resolveAndApplyAssignments(
    tm: TranslationMemory,
    organizationId: Long,
    assignedProjects: List<ProjectAssignmentDto>?,
    assignedProjectIds: Set<Long>?,
  ) {
    // assignedProjects takes precedence over assignedProjectIds
    if (assignedProjects != null) {
      updateAssignedProjectsWithAccess(tm, organizationId, assignedProjects)
      return
    }
    val ids = assignedProjectIds ?: return
    updateAssignedProjectsWithAccess(
      tm,
      organizationId,
      ids.map { id ->
        ProjectAssignmentDto().apply {
          projectId = id
          readAccess = true
          writeAccess = true
        }
      },
    )
  }

  private fun updateAssignedProjectsWithAccess(
    tm: TranslationMemory,
    organizationId: Long,
    assignments: List<ProjectAssignmentDto>,
  ) {
    val existing = translationMemoryProjectRepository.findByTranslationMemoryId(tm.id)
    val existingByProjectId = existing.associateBy { it.project.id }
    val newProjectIds = assignments.map { it.projectId }.toSet()

    // Remove assignments not in the new set
    existing.filter { it.project.id !in newProjectIds }.forEach {
      translationMemoryProjectRepository.delete(it)
    }

    for (dto in assignments) {
      val existingAssignment = existingByProjectId[dto.projectId]
      if (existingAssignment != null) {
        // Update access settings
        existingAssignment.readAccess = dto.readAccess
        existingAssignment.writeAccess = dto.writeAccess
        existingAssignment.penalty = dto.penalty
        translationMemoryProjectRepository.save(existingAssignment)
      } else {
        // Create new assignment
        val project = projectService.get(dto.projectId)
        require(project.organizationOwner.id == organizationId) {
          "Project ${dto.projectId} does not belong to organization $organizationId"
        }
        val projectBaseTag = project.baseLanguage?.tag
        if (projectBaseTag != null && projectBaseTag != tm.sourceLanguageTag) {
          throw BadRequestException(
            Message.TRANSLATION_MEMORY_BASE_LANGUAGE_MISMATCH,
            listOf(tm.name, tm.sourceLanguageTag, projectBaseTag),
          )
        }
        // Stack under every existing assignment (project TM starts at 0, previous shared TMs at
        // 1..N). First assignment to an empty-of-shared project ends up at 1.
        val nextPriority =
          translationMemoryProjectRepository
            .findByProjectId(dto.projectId)
            .maxOfOrNull { it.priority }
            ?.plus(1) ?: 0
        val assignment = TranslationMemoryProject()
        assignment.translationMemory = tm
        assignment.project = project
        assignment.readAccess = dto.readAccess
        assignment.writeAccess = dto.writeAccess
        assignment.priority = nextPriority
        assignment.penalty = dto.penalty
        translationMemoryProjectRepository.save(assignment)
      }
    }
  }

  @Transactional
  fun delete(
    organizationId: Long,
    translationMemoryId: Long,
  ) {
    val tm = getShared(organizationId, translationMemoryId)
    translationMemoryRepository.delete(tm)
  }
}
