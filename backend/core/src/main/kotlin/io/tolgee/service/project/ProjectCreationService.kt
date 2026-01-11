package io.tolgee.service.project

import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.service.language.LanguageService
import io.tolgee.service.organization.OrganizationService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectCreationService(
  private val organizationService: OrganizationService,
  private val languageService: LanguageService,
  private val projectService: ProjectService,
) {
  @Transactional
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#result.id")
  fun createProject(dto: CreateProjectRequest): Project {
    val project = Project()
    project.name = dto.name
    project.icuPlaceholders = dto.icuPlaceholders

    project.organizationOwner = organizationService.get(dto.organizationId)

    project.slug = getSlug(dto)

    projectService.save(project)

    val createdLanguages = dto.languages!!.map { languageService.createLanguage(it, project) }
    project.baseLanguage = getOrAssignBaseLanguage(dto, createdLanguages)

    return project
  }

  private fun getSlug(dto: CreateProjectRequest): String {
    val desiredSlug = dto.slug ?: return projectService.generateSlug(dto.name, null)

    if (!projectService.validateSlugUniqueness(desiredSlug)) {
      throw BadRequestException(Message.SLUG_NOT_UNIQUE)
    }

    return desiredSlug
  }

  private fun getOrAssignBaseLanguage(
    dto: CreateProjectRequest,
    createdLanguages: List<Language>,
  ): Language {
    if (dto.baseLanguageTag != null) {
      return createdLanguages.find { it.tag == dto.baseLanguageTag }
        ?: throw BadRequestException(Message.LANGUAGE_WITH_BASE_LANGUAGE_TAG_NOT_FOUND)
    }
    return createdLanguages[0]
  }
}
