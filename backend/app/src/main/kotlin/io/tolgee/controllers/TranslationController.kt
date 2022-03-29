@file:Suppress("MVCPathVariableInspection")

package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.dtos.response.KeyWithTranslationsResponseDto
import io.tolgee.dtos.response.ViewDataResponse
import io.tolgee.dtos.response.translations_view.ResponseParams
import io.tolgee.model.Permission
import io.tolgee.model.enums.ApiScope
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.KeyService
import io.tolgee.service.SecurityService
import io.tolgee.service.TranslationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/project/{projectId:[0-9]+}/translations", "/api/project/translations")
@Tag(name = "Translations", description = "Manipulates localization messages and metadata")
@Deprecated("Use V2TranslationController")
class TranslationController @Autowired constructor(
  private val translationService: TranslationService,
  private val keyService: KeyService,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
) : IController {
  @GetMapping(value = ["/{languages}"])
  @AccessWithAnyProjectPermission
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
  @Operation(summary = "Get all translations for specific languages")
  fun getTranslations(@PathVariable("languages") languages: Set<String>): Map<String, Any> {
    return translationService.getTranslations(languages, projectHolder.project.id)
  }

  @Suppress("DeprecatedCallableAddReplaceWith")
  @PostMapping("/set")
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
  @AccessWithProjectPermission(permission = Permission.ProjectPermissionType.TRANSLATE)
  @Deprecated(message = "Use put method to /api/project/{projectId}/translations or /api/project/translations")
  @Hidden
  fun setTranslationsPost(@RequestBody @Valid dto: SetTranslationsWithKeyDto) {
    setTranslations(dto)
  }

  @PutMapping("")
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
  @AccessWithProjectPermission(permission = Permission.ProjectPermissionType.TRANSLATE)
  @Operation(summary = "Sets translations for existing key")
  fun setTranslations(@RequestBody @Valid dto: SetTranslationsWithKeyDto) {
    val key = keyService.get(
      projectHolder.project.id,
      dto.key
    )
    securityService.checkLanguageTagPermissions(dto.translations.keys, projectHolder.project.id)
    translationService.setForKey(key, dto.translations)
  }

  @PostMapping("")
  @AccessWithApiKey([ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_EDIT])
  @AccessWithProjectPermission(permission = Permission.ProjectPermissionType.EDIT)
  @Operation(summary = "Sets translations for existing or not existing key")
  fun createOrUpdateTranslations(@RequestBody @Valid dto: SetTranslationsWithKeyDto) {
    val project = projectHolder.projectEntity
    val key = keyService.getOrCreateKey(project, PathDTO.fromFullPath(dto.key))
    translationService.setForKey(key, dto.translations)
  }

  @GetMapping(value = ["/view"])
  @Operation(summary = "Returns data for translations view with metadata")
  fun getViewData(
    @PathVariable("projectId") projectId: Long?,
    @RequestParam(name = "languages", required = false) languages: Set<String>?,
    @RequestParam(name = "limit", defaultValue = "10") limit: Int,
    @RequestParam(name = "offset", defaultValue = "0") offset: Int,
    @RequestParam(name = "search", required = false) search: String?
  ): ViewDataResponse<LinkedHashSet<KeyWithTranslationsResponseDto>, ResponseParams> {
    securityService.checkProjectPermission(projectId!!, Permission.ProjectPermissionType.VIEW)
    return translationService.getViewDataOld(languages, projectId, limit, offset, search)
  }
}
