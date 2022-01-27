package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.key.DeprecatedEditKeyDTO
import io.tolgee.dtos.request.key.OldEditKeyDto
import io.tolgee.dtos.request.translation.GetKeyTranslationsReqDto
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.dtos.response.DeprecatedKeyDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.model.enums.ApiScope
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.KeyService
import io.tolgee.service.SecurityService
import io.tolgee.service.TranslationService
import org.springframework.transaction.annotation.Transactional
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

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/api/project/{projectId}/sources",
    "/api/project/{projectId}/keys",
    "/api/project/keys"
  ]
)
@Deprecated("Use V2KeyController")
@Tag(name = "Localization keys", description = "Manipulates localization keys and their translations and metadata")
class KeyController(
  private val keyService: KeyService,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val translationService: TranslationService,
) : IController {

  @PostMapping(value = ["/create", ""])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @Operation(summary = "Creates new key with specified translation data")
  fun create(
    @PathVariable("projectId") projectId: Long?,
    @RequestBody @Valid dto: SetTranslationsWithKeyDto?
  ) {
    keyService.create(projectHolder.projectEntity, dto!!)
  }

  @PostMapping(value = ["/edit"])
  @Operation(summary = "Edits key name")
  @Deprecated("Uses wrong naming in body object, use \"PUT .\" - will be removed in 2.0")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun editDeprecated(
    @PathVariable("projectId") projectId: Long?,
    @RequestBody @Valid dto: DeprecatedEditKeyDTO?
  ) {
    keyService.edit(projectHolder.projectEntity, dto!!)
  }

  @PutMapping(value = [""])
  @Operation(summary = "Edits key name")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun edit(@PathVariable("projectId") projectId: Long?, @RequestBody @Valid dto: OldEditKeyDto) {
    keyService.edit(projectHolder.project.id, dto)
  }

  @GetMapping(value = ["{id}"])
  @Operation(summary = "Returns key with specified id")
  @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
  fun getDeprecated(@PathVariable("id") id: Long?): DeprecatedKeyDto {
    val key = keyService.findOptional(id!!).orElseThrow { NotFoundException() }
    securityService.checkAnyProjectPermission(key.project!!.id)
    return DeprecatedKeyDto(key.name)
  }

  @DeleteMapping(value = ["/{id}"])
  @Operation(summary = "Deletes key with specified id")
  fun delete(@PathVariable id: Long?) {
    val key = keyService.findOptional(id!!).orElseThrow { NotFoundException() }
    securityService.checkProjectPermission(key.project!!.id, Permission.ProjectPermissionType.EDIT)
    keyService.delete(id)
  }

  @DeleteMapping(value = [""])
  @Transactional
  @Operation(summary = "Deletes multiple keys by their IDs")
  fun delete(@RequestBody ids: Set<Long>?) {
    for (key in keyService.findOptional(ids!!)) {
      securityService.checkProjectPermission(key.project!!.id, Permission.ProjectPermissionType.EDIT)
    }
    keyService.deleteMultiple(ids)
  }

  @PostMapping(value = ["/translations/{languages}"])
  @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
  @AccessWithAnyProjectPermission
  @Operation(
    summary = "Returns translations for specific key by its name",
    description = "Key name must be provided in method body, since it can be long and can contain characters hard to " +
      "encode"
  )
  fun getKeyTranslationsPost(
    @RequestBody body: GetKeyTranslationsReqDto,
    @PathVariable("languages") languages: Set<String>?
  ): Map<String, String?> {
    val projectId = projectHolder.project.id
    val pathDTO = PathDTO.fromFullPath(body.key)
    return translationService.getKeyTranslationsResult(projectId, pathDTO, languages)
  }
}
