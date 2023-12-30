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
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.language.LanguageModel
import io.tolgee.hateoas.language.LanguageModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.LanguageView
import io.tolgee.model.views.LanguageViewImpl
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
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
  private val securityService: SecurityService,
  private val languageModelAssembler: LanguageModelAssembler,
  private val pagedAssembler: PagedResourcesAssembler<LanguageView>,
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
    dto: LanguageDto,
  ): LanguageModel {
    val project = projectService.get(projectId)
    languageValidator.validateCreate(dto, project)
    val language = languageService.createLanguage(dto, project)
    return languageModelAssembler.toModel(LanguageViewImpl(language, false))
  }

  @Operation(summary = "Edits language")
  @PutMapping(value = ["/{languageId}"])
  @RequestActivity(ActivityType.EDIT_LANGUAGE)
  @RequiresProjectPermissions([Scope.LANGUAGES_EDIT])
  @AllowApiAccess
  fun editLanguage(
    @RequestBody @Valid
    dto: LanguageDto,
    @PathVariable("languageId") languageId: Long,
  ): LanguageModel {
    languageValidator.validateEdit(languageId, dto)
    val view = languageService.getView(languageId)
    languageService.editLanguage(view.language, dto)
    val languageView = languageService.getView(languageId)
    return languageModelAssembler.toModel(languageView)
  }

  @GetMapping(value = [""])
  @Operation(summary = "Returns all project languages", tags = ["Languages"])
  @UseDefaultPermissions
  @AllowApiAccess
  fun getAll(
    @PathVariable("projectId") pathProjectId: Long?,
    @ParameterObject pageable: Pageable,
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
    val languageView = languageService.getView(id)
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
    val language = languageService.get(languageId)
    securityService.checkProjectPermission(language.project.id, Scope.LANGUAGES_EDIT)

    // if base language is missing, select first language
    val baseLanguage = projectService.getOrCreateBaseLanguage(projectHolder.project.id)
    if (baseLanguage!!.id == languageId) {
      throw BadRequestException(Message.CANNOT_DELETE_BASE_LANGUAGE)
    }
    languageService.deleteLanguage(languageId)
  }
}
