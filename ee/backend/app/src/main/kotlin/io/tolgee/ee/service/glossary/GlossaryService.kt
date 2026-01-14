package io.tolgee.ee.service.glossary

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.ee.data.glossary.CreateGlossaryRequest
import io.tolgee.ee.data.glossary.GlossaryWithStats
import io.tolgee.ee.data.glossary.UpdateGlossaryRequest
import io.tolgee.ee.repository.glossary.GlossaryRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.glossary.Glossary
import io.tolgee.service.GlossaryCleanupService
import io.tolgee.service.project.ProjectService
import jakarta.transaction.Transactional
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Primary
@Service
class GlossaryService(
  private val glossaryRepository: GlossaryRepository,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
  private val projectService: ProjectService,
  private val currentDateProvider: CurrentDateProvider,
) : GlossaryCleanupService {
  fun findAll(organizationId: Long): List<Glossary> {
    return glossaryRepository.findByOrganizationId(organizationId)
  }

  fun findAllPaged(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<Glossary> {
    return glossaryRepository.findByOrganizationIdPaged(organizationId, pageable, search)
  }

  fun findAllWithStatsPaged(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<GlossaryWithStats> {
    return glossaryRepository.findByOrganizationIdWithStatsPaged(organizationId, pageable, search)
  }

  fun find(
    organizationId: Long,
    glossaryId: Long,
  ): Glossary? {
    return glossaryRepository.find(organizationId, glossaryId)
  }

  fun get(
    organizationId: Long,
    glossaryId: Long,
  ): Glossary {
    return find(organizationId, glossaryId) ?: throw NotFoundException(Message.GLOSSARY_NOT_FOUND)
  }

  @Transactional
  fun create(
    organization: Organization,
    dto: CreateGlossaryRequest,
  ): Glossary {
    val glossary =
      Glossary(
        name = dto.name,
        baseLanguageTag = dto.baseLanguageTag,
      ).apply {
        organizationOwner = organization

        updateAssignedProjects(this, dto.assignedProjectIds ?: emptySet())
      }
    return glossaryRepository.save(glossary)
  }

  @Transactional
  fun update(
    organizationId: Long,
    glossaryId: Long,
    dto: UpdateGlossaryRequest,
  ): Glossary {
    val glossary = get(organizationId, glossaryId)
    glossary.name = dto.name
    if (dto.baseLanguageTag != glossary.baseLanguageTag) {
      glossaryTermTranslationService.updateBaseLanguage(
        glossary,
        glossary.baseLanguageTag,
        dto.baseLanguageTag,
      )
    }
    glossary.baseLanguageTag = dto.baseLanguageTag
    val newAssignedProjects = dto.assignedProjectIds
    if (newAssignedProjects != null) {
      updateAssignedProjects(glossary, newAssignedProjects)
    }
    return glossaryRepository.save(glossary)
  }

  private fun updateAssignedProjects(
    glossary: Glossary,
    newAssignedProjects: Iterable<Long>,
  ) {
    glossary.assignedProjects.clear()
    val projects = projectService.findAll(newAssignedProjects)
    projects.forEach {
      if (it.organizationOwner.id != glossary.organizationOwner.id) {
        // Project belongs to another organization
        throw NotFoundException(Message.PROJECT_NOT_FOUND)
      }
    }
    glossary.assignedProjects.addAll(projects)
  }

  @Transactional
  fun delete(
    organizationId: Long,
    glossaryId: Long,
  ) {
    val glossary = get(organizationId, glossaryId)
    glossaryRepository.delete(glossary)
  }

  fun getAssignedProjectsIds(glossary: Glossary): Set<Long> {
    return glossaryRepository.findAssignedProjectsIdsByGlossaryId(glossary.id)
  }

  @Transactional
  fun assignProject(
    organizationId: Long,
    glossaryId: Long,
    projectId: Long,
  ) {
    val glossary = get(organizationId, glossaryId)
    val project = projectService.get(projectId)
    if (project.organizationOwner.id != organizationId) {
      // Project belongs to another organization
      throw NotFoundException(Message.PROJECT_NOT_FOUND)
    }
    glossary.assignedProjects.add(project)
    glossaryRepository.save(glossary)
  }

  @Transactional
  fun unassignProject(
    organizationId: Long,
    glossaryId: Long,
    projectId: Long,
  ) {
    glossaryRepository.unassignProject(organizationId, glossaryId, projectId)
  }

  override fun unassignFromAllProjects(project: Project) {
    glossaryRepository.unassignProjectFromAll(project.id)
  }
}
