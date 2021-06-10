@file:Suppress("MVCPathVariableInspection")

package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import io.tolgee.constants.Message
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.dtos.request.validators.LanguageValidator
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.service.LanguageService
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = [
    "/api/project/{projectId:[0-9]+}/languages",
    "/api/project/languages"
])
@Tags(value = [
    Tag(name = "Languages", description = "Languages"),
])
open class LanguageController(
        private val languageService: LanguageService,
        private val projectService: ProjectService,
        private val languageValidator: LanguageValidator,
        private val securityService: SecurityService,
        private val authenticationFacade: AuthenticationFacade
) : IController {

    @PostMapping(value = [""])
    @Operation(summary = "Creates language")
    fun createLanguage(@PathVariable("projectId") projectId: Long,
                       @RequestBody @Valid dto: LanguageDto?): LanguageDto {
        val project = projectService.get(projectId).orElseThrow { NotFoundException() }
        securityService.checkProjectPermission(projectId, Permission.ProjectPermissionType.MANAGE)
        languageValidator.validateCreate(dto, project)
        val language = languageService.createLanguage(dto, project!!)
        return LanguageDto.fromEntity(language)
    }

    @Operation(summary = "Edits language")
    @PostMapping(value = ["/edit"])
    fun editLanguage(@RequestBody @Valid dto: LanguageDto?): LanguageDto {
        languageValidator.validateEdit(dto)
        val language = languageService.findById(dto!!.id!!).orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }
        securityService.checkProjectPermission(language.project!!.id, Permission.ProjectPermissionType.MANAGE)
        return LanguageDto.fromEntity(languageService.editLanguage(dto))
    }

    @GetMapping(value = [""])
    @AccessWithApiKey
    @Operation(summary = "Returns all project languages", tags = ["API KEY", "Languages"])
    fun getAll(@PathVariable("projectId") pathProjectId: Long?): Set<LanguageDto> {
        val projectId = if (pathProjectId === null) authenticationFacade.apiKey.project!!.id else pathProjectId
        securityService.checkAnyProjectPermission(projectId)
        return languageService.findAll(projectId).stream().map { LanguageDto.fromEntity(it) }
                .collect(Collectors.toCollection { LinkedHashSet() })
    }

    @GetMapping(value = ["{id}"])
    @Operation(summary = "Returns specific language")
    operator fun get(@PathVariable("id") id: Long?): LanguageDto {
        val language = languageService.findById(id!!).orElseThrow { NotFoundException() }
        securityService.checkAnyProjectPermission(language.project!!.id)
        return LanguageDto.fromEntity(language)
    }

    @Operation(summary = "Deletes specific language")
    @DeleteMapping(value = ["/{id}"])
    fun deleteLanguage(@PathVariable id: Long) {
        val language = languageService.findById(id).orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }
        securityService.checkProjectPermission(language.project!!.id, Permission.ProjectPermissionType.MANAGE)
        languageService.deleteLanguage(id)
    }
}
