package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.key.KeyModel
import io.tolgee.api.v2.hateoas.key.KeyModelAssembler
import io.tolgee.constants.ApiScope
import io.tolgee.constants.Message
import io.tolgee.controllers.IController
import io.tolgee.dtos.request.CreateKeyDto
import io.tolgee.dtos.request.EditKeyDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.model.key.Key
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.KeyService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = [
    "/v2/projects/{projectId}/keys",
    "/v2/projects/keys"
])
@Tag(name = "Localization keys", description = "Manipulates localization keys and their translations and metadata")
class V2KeyController(
        private val keyService: KeyService,
        private val projectHolder: ProjectHolder,
        private val keyModelAssembler: KeyModelAssembler
) : IController {

    @PostMapping(value = ["/create", ""])
    @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
    @AccessWithApiKey(scopes = [ApiScope.KEYS_EDIT])
    @Operation(summary = "Creates new key")
    fun create(@RequestBody @Valid dto: CreateKeyDto): KeyModel {
        return keyService.create(projectHolder.project, dto.name).model
    }

    @PutMapping(value = [""])
    @Operation(summary = "Edits key name")
    @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
    @AccessWithApiKey(scopes = [ApiScope.KEYS_EDIT])
    fun edit(@RequestBody @Valid dto: EditKeyDto): KeyModel {
        return keyService.edit(projectHolder.project, dto).model
    }

    @DeleteMapping(value = ["/{id}"])
    @Operation(summary = "Deletes key with specified id")
    @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
    @AccessWithApiKey(scopes = [ApiScope.KEYS_EDIT])
    fun delete(@PathVariable id: Long) {
        keyService.get(id).orElseThrow { NotFoundException() }.checkInProject()
        keyService.delete(id)
    }

    @DeleteMapping(value = [""])
    @Transactional
    @Operation(summary = "Deletes multiple keys by IDs")
    @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
    @AccessWithApiKey(scopes = [ApiScope.KEYS_EDIT])
    fun delete(@RequestBody ids: Set<Long>) {
        keyService.get(ids).forEach { it.checkInProject() }
        keyService.deleteMultiple(ids)
    }

    private fun Key.checkInProject() {
        if (this.project!!.id != projectHolder.project.id) {
            throw BadRequestException(Message.KEY_NOT_FROM_PROJECT)
        }
    }

    private val Key.model: KeyModel
        get() = keyModelAssembler.toModel(this)
}
