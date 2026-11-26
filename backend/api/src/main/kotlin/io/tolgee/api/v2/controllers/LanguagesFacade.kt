package io.tolgee.api.v2.controllers

import io.tolgee.api.mcp.McpFacade
import io.tolgee.api.mcp.checkValid
import io.tolgee.component.LanguageValidator
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.language.LanguageFilters
import io.tolgee.model.Language
import io.tolgee.security.ProjectHolder
import io.tolgee.service.language.LanguageService
import jakarta.validation.Validator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class LanguagesFacade(
  private val languageService: LanguageService,
  private val languageValidator: LanguageValidator,
  private val projectHolder: ProjectHolder,
  private val validator: Validator,
) : McpFacade {
  fun getPaged(
    pageable: Pageable,
    filters: LanguageFilters?,
  ): Page<LanguageDto> = languageService.getPaged(projectHolder.project.id, pageable, filters)

  fun findByTag(tag: String): LanguageDto? = languageService.findByTag(tag, projectHolder.project.id)

  fun createLanguage(dto: LanguageRequest): Language {
    validator.checkValid(dto)
    val project = projectHolder.projectEntity
    languageValidator.validateCreate(dto, project)
    return languageService.createLanguage(dto, project)
  }
}
