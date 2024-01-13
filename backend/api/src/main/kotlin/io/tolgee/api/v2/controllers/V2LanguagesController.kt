/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.LanguageValidator
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.language.LanguageModel
import io.tolgee.hateoas.language.LanguageModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.LanguageService
import io.tolgee.service.project.ProjectService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/languages",
    "/v2/projects/languages",
  ],
)
@Tags(
  value = [
    Tag(name = "Languages", description = "Languages"),
  ],
)
class V2LanguagesController(
  private val languageService: LanguageService,
  private val projectService: ProjectService,
  private val languageValidator: LanguageValidator,
  private val languageModelAssembler: LanguageModelAssembler,
  private val pagedAssembler: PagedResourcesAssembler<LanguageDto>,
  private val projectHolder: ProjectHolder,
) : IController {
  @PostMapping(value = [""])
  @Operation(summary = "Creates language")
  @RequestActivity(ActivityType.CREATE_LANGUAGE)
  @RequiresProjectPermissions([Scope.LANGUAGES_EDIT])
  @AllowApiAccess
  fun createLanguage(
    @PathVariable("projectId") projectId: Long,
    @RequestBody @Valid
    dto: LanguageRequest,
  ): LanguageModel {
    val project = projectService.get(projectId)
    languageValidator.validateCreate(dto, project)
    val language = languageService.createLanguage(dto, project)
    return languageModelAssembler.toModel(LanguageDto.fromEntity(language, project.baseLanguage?.id))
  }

  @Operation(summary = "Edits language")
  @PutMapping(value = ["/{languageId}"])
  @RequestActivity(ActivityType.EDIT_LANGUAGE)
  @RequiresProjectPermissions([Scope.LANGUAGES_EDIT])
  @AllowApiAccess
  fun editLanguage(
    @RequestBody @Valid
    dto: LanguageRequest,
    @PathVariable("languageId") languageId: Long,
  ): LanguageModel {
    languageValidator.validateEdit(languageId, dto)
    languageService.editLanguage(languageService.getEntity(languageId, projectHolder.project.id), dto)
    return languageModelAssembler.toModel(languageService.get(languageId, projectHolder.project.id))
  }

  @GetMapping(value = [""])
  @Operation(summary = "Returns all project languages", tags = ["Languages"])
  @UseDefaultPermissions
  @AllowApiAccess
  fun getAll(
    @PathVariable("projectId") pathProjectId: Long?,
    @ParameterObject @SortDefault("tag") pageable: Pageable,
  ): PagedModel<LanguageModel> {
    val data = languageService.getPaged(projectHolder.project.id, pageable)
    return pagedAssembler.toModel(data, languageModelAssembler)
  }

  @GetMapping(value = ["/{languageId}"])
  @Operation(summary = "Returns specific language")
  @UseDefaultPermissions
  @AllowApiAccess
  fun get(
    @PathVariable("languageId") id: Long,
  ): LanguageModel {
    val languageView = languageService.get(id, projectHolder.project.id)
    return languageModelAssembler.toModel(languageView)
  }

  @Operation(summary = "Deletes specific language")
  @DeleteMapping(value = ["/{languageId}"])
  @RequestActivity(ActivityType.DELETE_LANGUAGE)
  @RequiresProjectPermissions([Scope.LANGUAGES_EDIT])
  @AllowApiAccess
  fun deleteLanguage(
    @PathVariable languageId: Long,
  ) {
    val isBaseLanguage =
      languageService.getProjectLanguages(projectHolder.project.id).any { it.base && it.id == languageId }

    if (isBaseLanguage) {
      throw BadRequestException(Message.CANNOT_DELETE_BASE_LANGUAGE)
    }

    languageService.deleteLanguage(languageId, projectHolder.project.id)
  }
}
