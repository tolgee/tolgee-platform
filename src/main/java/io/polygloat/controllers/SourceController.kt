package io.polygloat.controllers

import io.polygloat.constants.Message
import io.polygloat.dtos.request.EditSourceDTO
import io.polygloat.dtos.request.SetTranslationsDTO
import io.polygloat.dtos.response.SourceDTO
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
@RequestMapping("/api/repository/{repositoryId}/sources")
open class SourceController(
        private val keyService: KeyService,
        private val securityService: SecurityService
) : IController {

    @PostMapping("/create")
    fun create(@PathVariable("repositoryId") repositoryId: Long?, @RequestBody dto: @Valid SetTranslationsDTO?) {
        val permission = securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.TRANSLATE)
        keyService.createSource(permission.repository!!, dto!!)
    }

    @PostMapping(value = ["/edit"])
    fun edit(@PathVariable("repositoryId") repositoryId: Long?, @RequestBody dto: @Valid EditSourceDTO?) {
        val permission = securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.EDIT)
        keyService.editSource(permission.repository!!, dto!!)
    }

    @GetMapping(value = ["{id}"])
    operator fun get(@PathVariable("id") id: Long?): SourceDTO {
        val key = keyService.getSource(id!!).orElseThrow { NotFoundException() }
        securityService.getAnyRepositoryPermission(key.repository!!.id)
        return SourceDTO(key.name)
    }

    @DeleteMapping(value = ["/{id}"])
    fun delete(@PathVariable id: Long?) {
        val key = keyService.getSource(id!!).orElseThrow { NotFoundException() }
        securityService.checkRepositoryPermission(key.repository!!.id, Permission.RepositoryPermissionType.EDIT)
        keyService.deleteSource(id)
    }

    @DeleteMapping(value = [""])
    @Transactional
    open fun delete(@PathVariable("repositoryId") repositoryId: Long, @RequestBody ids: Set<Long>?) {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.EDIT)
        for (key in keyService.getSources(ids!!)) {
            if (repositoryId != key.repository!!.id) {
                throw ValidationException(Message.SOURCE_NOT_FROM_REPOSITORY.code)
            }
            keyService.deleteSources(ids)
        }
    }
}