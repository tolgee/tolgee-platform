package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.key.KeyModel
import io.tolgee.api.v2.hateoas.key.KeyModelAssembler
import io.tolgee.controllers.IController
import io.tolgee.dtos.request.CreateKeyDto
import io.tolgee.dtos.request.EditKeyDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.key.Key
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.KeyService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/keys",
    "/v2/projects/keys"
  ]
)
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
  @ResponseStatus(HttpStatus.CREATED)
  fun create(@RequestBody @Valid dto: CreateKeyDto): ResponseEntity<KeyModel> {
    return ResponseEntity(keyService.create(projectHolder.project, dto.name).model, HttpStatus.CREATED)
  }

  @PutMapping(value = ["/{id}"])
  @Operation(summary = "Edits key name")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @AccessWithApiKey(scopes = [ApiScope.KEYS_EDIT])
  fun edit(@PathVariable id: Long, @RequestBody @Valid dto: EditKeyDto): KeyModel {
    val key = keyService.get(id).orElseThrow { NotFoundException() }
    key.checkInProject()
    return keyService.edit(projectHolder.project, id, dto).model
  }

  @DeleteMapping(value = ["/{ids:[0-9,]+}"])
  @Transactional
  @Operation(summary = "Deletes one or multiple keys by their IDs")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @AccessWithApiKey(scopes = [ApiScope.KEYS_EDIT])
  fun delete(@PathVariable ids: Set<Long>) {
    keyService.get(ids).forEach { it.checkInProject() }
    keyService.deleteMultiple(ids)
  }

  private fun Key.checkInProject() {
    keyService.checkInProject(this, projectHolder.project)
  }

  private val Key.model: KeyModel
    get() = keyModelAssembler.toModel(this)
}
