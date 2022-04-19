/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import io.tolgee.activity.ActivityType
import io.tolgee.activity.RequestActivity
import io.tolgee.api.v2.hateoas.organization.LanguageModel
import io.tolgee.api.v2.hateoas.organization.LanguageModelAssembler
import io.tolgee.component.LanguageValidator
import io.tolgee.constants.Message
import io.tolgee.controllers.IController
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.LanguageService
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import org.springdoc.api.annotations.ParameterObject
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
import javax.validation.Valid

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@Tag(name = "Import")
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/languages",
    "/v2/projects/languages"
  ]
)
@Tags(
  value = [
    Tag(name = "Languages", description = "Languages"),
  ]
)
class V2LanguagesController(
  private val languageService: LanguageService,
  private val projectService: ProjectService,
  private val languageValidator: LanguageValidator,
  private val securityService: SecurityService,
  private val languageModelAssembler: LanguageModelAssembler,
  private val pagedAssembler: PagedResourcesAssembler<Language>,
  private val projectHolder: ProjectHolder,
) : IController {

  @PostMapping(value = [""])
  @Operation(summary = "Creates language")
  @RequestActivity(ActivityType.CREATE_LANGUAGE)
  fun createLanguage(
    @PathVariable("projectId") projectId: Long,
    @RequestBody @Valid dto: LanguageDto
  ): LanguageModel {
    val project = projectService.get(projectId)
    securityService.checkProjectPermission(projectId, Permission.ProjectPermissionType.MANAGE)
    languageValidator.validateCreate(dto, project)
    val language = languageService.createLanguage(dto, project)
    return languageModelAssembler.toModel(language)
  }

  @Operation(summary = "Edits language")
  @PutMapping(value = ["/{languageId}"])
  @RequestActivity(ActivityType.EDIT_LANGUAGE)
  fun editLanguage(
    @RequestBody @Valid dto: LanguageDto,
    @PathVariable("languageId") languageId: Long
  ): LanguageModel {
    languageValidator.validateEdit(languageId, dto)
    val language = languageService.findById(languageId).orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }
    securityService.checkProjectPermission(language.project!!.id, Permission.ProjectPermissionType.MANAGE)
    return languageModelAssembler.toModel(languageService.editLanguage(languageId, dto))
  }

  @GetMapping(value = [""])
  @AccessWithApiKey
  @AccessWithAnyProjectPermission
  @Operation(summary = "Returns all project languages", tags = ["API KEY", "Languages"])
  fun getAll(
    @PathVariable("projectId") pathProjectId: Long?,
    @ParameterObject pageable: Pageable
  ): PagedModel<LanguageModel> {
    val data = languageService.getPaged(projectHolder.project.id, pageable)
    return pagedAssembler.toModel(data, languageModelAssembler)
  }

  @GetMapping(value = ["{languageId}"])
  @Operation(summary = "Returns specific language")
  @AccessWithAnyProjectPermission
  operator fun get(@PathVariable("languageId") id: Long?): LanguageModel {
    val language = languageService.findById(id!!).orElseThrow { NotFoundException() }
    securityService.checkAnyProjectPermission(language.project.id)
    return languageModelAssembler.toModel(language)
  }

  @Operation(summary = "Deletes specific language")
  @DeleteMapping(value = ["/{languageId}"])
  @RequestActivity(ActivityType.DELETE_LANGUAGE)
  fun deleteLanguage(@PathVariable languageId: Long) {
    val language = languageService.findById(languageId)
      .orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }
    securityService.checkProjectPermission(language.project.id, Permission.ProjectPermissionType.MANAGE)

    // if base language is missing, select first language
    val baseLanguage = projectService.getOrCreateBaseLanguage(projectHolder.project.id)
    if (baseLanguage!!.id == languageId) {
      throw BadRequestException(Message.CANNOT_DELETE_BASE_LANGUAGE)
    }
    languageService.deleteLanguage(languageId)
  }
}
