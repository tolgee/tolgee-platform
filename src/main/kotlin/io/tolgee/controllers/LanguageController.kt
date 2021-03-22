package io.tolgee.controllers

import io.tolgee.constants.Message
import io.tolgee.dtos.request.LanguageDTO
import io.tolgee.dtos.request.validators.LanguageValidator
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AllowAccessWithApiKey
import io.tolgee.service.LanguageService
import io.tolgee.service.RepositoryService
import io.tolgee.service.SecurityService
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/api/repository/{repositoryId:[0-9]+}/languages", "/api/languages"])
open class LanguageController(
        private val languageService: LanguageService,
        private val repositoryService: RepositoryService,
        private val languageValidator: LanguageValidator,
        private val securityService: SecurityService,
        private val authenticationFacade: AuthenticationFacade
) : IController {
    @PostMapping(value = [""])
    fun createLanguage(@PathVariable("repositoryId") repositoryId: Long,
                       @RequestBody @Valid dto: LanguageDTO?): LanguageDTO {
        val repository = repositoryService.findById(repositoryId).orElseThrow { NotFoundException() }
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.MANAGE)
        languageValidator.validateCreate(dto, repository)
        val language = languageService.createLanguage(dto, repository)
        return LanguageDTO.fromEntity(language)
    }

    @PostMapping(value = ["/edit"])
    fun editLanguage(@RequestBody @Valid dto: LanguageDTO?): LanguageDTO {
        languageValidator.validateEdit(dto)
        val language = languageService.findById(dto!!.id).orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }
        securityService.checkRepositoryPermission(language.repository!!.id, Permission.RepositoryPermissionType.MANAGE)
        return LanguageDTO.fromEntity(LanguageService.editLanguage(dto))
    }

    @GetMapping(value = [""])
    @AllowAccessWithApiKey
    fun getAll(@PathVariable("repositoryId") pathRepositoryId: Long?): Set<LanguageDTO> {
        val repositoryId = if(pathRepositoryId === null) authenticationFacade.apiKey.repository!!.id else pathRepositoryId
        securityService.getAnyRepositoryPermission(repositoryId)
        return languageService.findAll(repositoryId).stream().map { LanguageDTO.fromEntity(it) }
                .collect(Collectors.toCollection { LinkedHashSet() })
    }

    @GetMapping(value = ["{id}"])
    operator fun get(@PathVariable("id") id: Long?): LanguageDTO {
        val language = languageService.findById(id).orElseThrow { NotFoundException() }
        securityService.getAnyRepositoryPermission(language.repository!!.id)
        return LanguageDTO.fromEntity(language)
    }

    @DeleteMapping(value = ["/{id}"])
    fun deleteLanguage(@PathVariable id: Long?) {
        val language = languageService.findById(id).orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }
        securityService.checkRepositoryPermission(language.repository!!.id, Permission.RepositoryPermissionType.MANAGE)
        languageService.deleteLanguage(id)
    }
}