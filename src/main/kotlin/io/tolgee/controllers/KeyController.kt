package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.ApiScope
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.DeprecatedEditKeyDTO
import io.tolgee.dtos.request.EditKeyDTO
import io.tolgee.dtos.request.GetKeyTranslationsReqDto
import io.tolgee.dtos.request.SetTranslationsDTO
import io.tolgee.dtos.response.DeprecatedKeyDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.openapi_fixtures.InternalIgnorePaths
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.repository_auth.AccessWithAnyRepositoryPermission
import io.tolgee.security.repository_auth.AccessWithRepositoryPermission
import io.tolgee.security.repository_auth.RepositoryHolder
import io.tolgee.service.KeyService
import io.tolgee.service.SecurityService
import io.tolgee.service.TranslationService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = [
    "/api/repository/{repositoryId}/sources",
    "/api/repository/{repositoryId}/keys",
    "/api/repository/keys"
])
@InternalIgnorePaths(["/api/repository/keys"])
@Tag(name = "Localization keys", description = "Manipulates localization keys and their translations and metadata")
open class KeyController(
        private val keyService: KeyService,
        private val securityService: SecurityService,
        private val repositoryHolder: RepositoryHolder,
        private val translationService: TranslationService,
) : IController {

    @PostMapping(value = ["/create", ""])
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.TRANSLATE)
    @Operation(summary = "Creates new key with specified translation data")
    open fun create(@PathVariable("repositoryId") repositoryId: Long?, @RequestBody @Valid dto: SetTranslationsDTO?) {
        keyService.create(repositoryHolder.repository, dto!!)
    }

    @PostMapping(value = ["/edit"])
    @Operation(summary = "Edits key name")
    @Deprecated("Uses wrong naming in body object, use \"PUT .\" - will be removed in 2.0")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    open fun editDeprecated(@PathVariable("repositoryId") repositoryId: Long?, @RequestBody @Valid dto: DeprecatedEditKeyDTO?) {
        keyService.edit(repositoryHolder.repository, dto!!)
    }

    @PutMapping(value = [""])
    @Operation(summary = "Edits key name")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    open fun edit(@PathVariable("repositoryId") repositoryId: Long?, @RequestBody @Valid dto: EditKeyDTO) {
        keyService.edit(repositoryHolder.repository, dto)
    }

    @GetMapping(value = ["{id}"])
    @Operation(summary = "Returns key with specified id")
    @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    open fun getDeprecated(@PathVariable("id") id: Long?): DeprecatedKeyDto {
        val key = keyService.get(id!!).orElseThrow { NotFoundException() }
        securityService.getAnyRepositoryPermissionOrThrow(key.repository!!.id)
        return DeprecatedKeyDto(key.name)
    }

    @DeleteMapping(value = ["/{id}"])
    @Operation(summary = "Deletes key with specified id")
    open fun delete(@PathVariable id: Long?) {
        val key = keyService.get(id!!).orElseThrow { NotFoundException() }
        securityService.checkRepositoryPermission(key.repository!!.id, Permission.RepositoryPermissionType.EDIT)
        keyService.delete(id)
    }

    @DeleteMapping(value = [""])
    @Transactional
    @Operation(summary = "Deletes multiple kdys by their IDs")
    open fun delete(@RequestBody ids: Set<Long>?) {
        for (key in keyService.get(ids!!)) {
            securityService.checkRepositoryPermission(key.repository!!.id, Permission.RepositoryPermissionType.EDIT)
        }
        keyService.deleteMultiple(ids)
    }

    @PostMapping(value = ["/translations/{languages}"])
    @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    @AccessWithAnyRepositoryPermission
    @Operation(
            summary = "Returns translations for specific key by its name",
            description = "Key name must be provided in method body, since it can be long and can contain characters hard to " +
                    "encode")
    @InternalIgnorePaths(["/translations/{languages}"])
    open fun getKeyTranslationsPost(
            @RequestBody body: GetKeyTranslationsReqDto,
            @PathVariable("languages") languages: Set<String?>?
    ): Map<String, String> {
        val repositoryId = repositoryHolder.repository.id
        val pathDTO = PathDTO.fromFullPath(body.key)
        return translationService.getKeyTranslationsResult(repositoryId, pathDTO, languages)
    }
}
