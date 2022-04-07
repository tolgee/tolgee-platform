package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.ActivityType
import io.tolgee.activity.RequestActivity
import io.tolgee.api.v2.hateoas.key.KeyModel
import io.tolgee.api.v2.hateoas.key.KeyModelAssembler
import io.tolgee.api.v2.hateoas.key.KeyWithDataModel
import io.tolgee.api.v2.hateoas.key.KeyWithDataModelAssembler
import io.tolgee.component.KeyComplexEditHelper
import io.tolgee.controllers.IController
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.key.Key
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.KeyService
import io.tolgee.service.SecurityService
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
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
  private val keyModelAssembler: KeyModelAssembler,
  private val keyWithDataModelAssembler: KeyWithDataModelAssembler,
  private val securityService: SecurityService,
  private val authenticationFacade: AuthenticationFacade,
  private val applicationContext: ApplicationContext
) : IController {
  @PostMapping(value = ["/create", ""])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @AccessWithApiKey(scopes = [ApiScope.KEYS_EDIT])
  @Operation(summary = "Creates new key")
  @ResponseStatus(HttpStatus.CREATED)
  @RequestActivity(ActivityType.CREATE_KEY)
  fun create(@RequestBody @Valid dto: CreateKeyDto): ResponseEntity<KeyWithDataModel> {
    if (dto.screenshotUploadedImageIds != null) {
      projectHolder.projectEntity.checkScreenshotsUploadPermission()
    }
    val key = keyService.create(projectHolder.projectEntity, dto)
    return ResponseEntity(keyWithDataModelAssembler.toModel(key), HttpStatus.CREATED)
  }

  @PutMapping(value = ["/{id}/complex-update"])
  @Operation(summary = "More")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.TRANSLATE)
  // key permissions are checked separately in method body
  @AccessWithApiKey([ApiScope.TRANSLATIONS_EDIT])
  @Transactional
  fun complexEdit(@PathVariable id: Long, @RequestBody @Valid dto: ComplexEditKeyDto): KeyWithDataModel {
    return KeyComplexEditHelper(applicationContext, id, dto).doComplexEdit()
  }

  @PutMapping(value = ["/{id}"])
  @Operation(summary = "Edits key name")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @AccessWithApiKey(scopes = [ApiScope.KEYS_EDIT])
  @RequestActivity(ActivityType.KEY_NAME_EDIT)
  fun edit(@PathVariable id: Long, @RequestBody @Valid dto: EditKeyDto): KeyModel {
    val key = keyService.findOptional(id).orElseThrow { NotFoundException() }
    key.checkInProject()
    return keyService.edit(id, dto).model
  }

  @DeleteMapping(value = ["/{ids:[0-9,]+}"])
  @Transactional
  @Operation(summary = "Deletes one or multiple keys by their IDs")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @AccessWithApiKey(scopes = [ApiScope.KEYS_EDIT])
  @RequestActivity(ActivityType.KEY_DELETE)
  fun delete(@PathVariable ids: Set<Long>) {
    keyService.findOptional(ids).forEach { it.checkInProject() }
    keyService.deleteMultiple(ids)
  }

  private fun Key.checkInProject() {
    keyService.checkInProject(this, projectHolder.project.id)
  }

  private val Key.model: KeyModel
    get() = keyModelAssembler.toModel(this)

  private fun Project.checkScreenshotsUploadPermission() {
    securityService.checkScreenshotsUploadPermission(this.id)
  }
}
