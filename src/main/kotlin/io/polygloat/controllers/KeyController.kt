package io.polygloat.controllers

import io.polygloat.constants.Message
import io.polygloat.dtos.request.EditKeyDTO
import io.polygloat.dtos.request.SetTranslationsDTO
import io.polygloat.dtos.response.KeyDTO
import io.polygloat.exceptions.NotFoundException
import io.polygloat.model.Permission
import io.polygloat.service.KeyService
import io.polygloat.service.SecurityService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.ValidationException

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/api/repository/{repositoryId}/sources", "/api/repository/{repositoryId}/keys"])
open class KeyController(
        private val keyService: KeyService,
        private val securityService: SecurityService
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
}