package io.tolgee.ee.service.glossary

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.ee.data.glossary.CreateGlossaryRequest
import io.tolgee.ee.data.glossary.UpdateGlossaryRequest
import io.tolgee.ee.repository.glossary.GlossaryRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.glossary.Glossary
import io.tolgee.service.project.ProjectService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GlossaryService(
  private val glossaryRepository: GlossaryRepository,
  private val projectService: ProjectService,
  private val currentDateProvider: CurrentDateProvider,
) {
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

  fun find(
    organizationId: Long,
    id: Long,
  ): Glossary? {
    return glossaryRepository.find(organizationId, id)
  }

  fun get(
    organizationId: Long,
    id: Long,
  ): Glossary {
    return find(organizationId, id) ?: throw NotFoundException(Message.GLOSSARY_NOT_FOUND)
  }

  fun create(
    organization: Organization,
    dto: CreateGlossaryRequest,
  ): Glossary {
    val glossary =
      Glossary(
        name = dto.name,
      ).apply {
        organizationOwner = organization
        baseLanguageCode = dto.baseLanguageCode

        // TODO project permissions handling?
        val projects = projectService.findAll(dto.assignedProjects ?: emptySet())
        assignedProjects.addAll(projects)
      }
    return glossaryRepository.save(glossary)
  }

  fun update(
    organizationId: Long,
    glossaryId: Long,
    dto: UpdateGlossaryRequest,
  ): Glossary {
    val glossary = get(organizationId, glossaryId)
    glossary.name = dto.name
    glossary.baseLanguageCode = dto.baseLanguageCode
    return glossaryRepository.save(glossary)
  }

  fun delete(
    organizationId: Long,
    glossaryId: Long,
  ) {
    glossaryRepository.softDelete(organizationId, glossaryId, currentDateProvider.date)
  }

  fun assignProject(
    organizationId: Long,
    glossaryId: Long,
    projectId: Long,
  ) {
    // TODO project permissions handling?
    val glossary = get(organizationId, glossaryId)
    val project = projectService.get(projectId)
    glossary.assignedProjects.add(project)
    glossaryRepository.save(glossary)
  }

  fun unassignProject(
    organizationId: Long,
    glossaryId: Long,
    projectId: Long,
  ) {
    glossaryRepository.unassignProject(organizationId, glossaryId, projectId)
  }
}
