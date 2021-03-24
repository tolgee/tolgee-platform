package io.tolgee.controllers

import io.tolgee.constants.ApiScope
import io.tolgee.constants.Message
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.EditKeyDTO
import io.tolgee.dtos.request.GetKeyTranslationsReqDto
import io.tolgee.dtos.request.SetTranslationsDTO
import io.tolgee.dtos.response.KeyDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.repository_auth.AccessWithAnyRepositoryPermission
import io.tolgee.security.repository_auth.RepositoryHolder
import io.tolgee.service.KeyService
import io.tolgee.service.SecurityService
import io.tolgee.service.TranslationService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.ValidationException

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = [
    "/api/repository/{repositoryId}/sources",
    "/api/repository/{repositoryId}/keys",
    "/api/repository/keys"
])
open class KeyController(
        private val keyService: KeyService,
        private val securityService: SecurityService,
        private val repositoryHolder: RepositoryHolder,
        private val translationService: TranslationService
) : IController {

    @PostMapping(value = ["/create", ""])
    open fun create(@PathVariable("repositoryId") repositoryId: Long?, @RequestBody @Valid dto: SetTranslationsDTO?) {
        val permission = securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.TRANSLATE)
        keyService.create(permission.repository!!, dto!!)
    }

    @PostMapping(value = ["/edit"])
    open fun edit(@PathVariable("repositoryId") repositoryId: Long?, @RequestBody @Valid dto: EditKeyDTO?) {
        val permission = securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.EDIT)
        keyService.edit(permission.repository!!, dto!!)
    }

    @GetMapping(value = ["{id}"])
    open fun get(@PathVariable("id") id: Long?): KeyDTO {
        val key = keyService.get(id!!).orElseThrow { NotFoundException() }
        securityService.getAnyRepositoryPermission(key.repository!!.id)
        return KeyDTO(key.name)
    }

    @DeleteMapping(value = ["/{id}"])
    open fun delete(@PathVariable id: Long?) {
        val key = keyService.get(id!!).orElseThrow { NotFoundException() }
        securityService.checkRepositoryPermission(key.repository!!.id, Permission.RepositoryPermissionType.EDIT)
        keyService.delete(id)
    }

    @DeleteMapping(value = [""])
    @Transactional
    open fun delete(@PathVariable("repositoryId") repositoryId: Long, @RequestBody ids: Set<Long>?) {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.EDIT)
        for (key in keyService.get(ids!!)) {
            if (repositoryId != key.repository!!.id) {
                throw ValidationException(Message.KEY_NOT_FROM_REPOSITORY.code)
            }
            keyService.deleteMultiple(ids)
        }
    }

    @PostMapping(value = ["/translations/{languages}"])
    @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    @AccessWithAnyRepositoryPermission
    open fun getKeyTranslationsPost(
            @RequestBody body: GetKeyTranslationsReqDto,
            @PathVariable("languages") languages: Set<String?>?
    ): Map<String, String> {
        val repositoryId = repositoryHolder.repository.id
        val pathDTO = PathDTO.fromFullPath(body.key)
        return translationService.getKeyTranslationsResult(repositoryId, pathDTO, languages)
    }
}
