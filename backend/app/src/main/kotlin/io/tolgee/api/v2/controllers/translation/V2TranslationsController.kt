/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.translation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import io.tolgee.api.v2.hateoas.translations.KeysWithTranslationsPageModel
import io.tolgee.api.v2.hateoas.translations.KeysWithTranslationsPagedResourcesAssembler
import io.tolgee.api.v2.hateoas.translations.SetTranslationsResponseModel
import io.tolgee.api.v2.hateoas.translations.TranslationModel
import io.tolgee.api.v2.hateoas.translations.TranslationModelAssembler
import io.tolgee.controllers.IController
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.dtos.request.translation.SelectAllResponse
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Screenshot
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.KeyService
import io.tolgee.service.LanguageService
import io.tolgee.service.ScreenshotService
import io.tolgee.service.SecurityService
import io.tolgee.service.TranslationService
import io.tolgee.service.query_builders.CursorUtil
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/translations",
    "/v2/projects/translations"
  ]
)
@Tags(
  value = [
    Tag(name = "Translations", description = "Operations related to translations in project"),
  ]
)
class V2TranslationsController(
  private val projectHolder: ProjectHolder,
  private val translationService: TranslationService,
  private val keyService: KeyService,
  private val pagedAssembler: KeysWithTranslationsPagedResourcesAssembler,
  private val translationModelAssembler: TranslationModelAssembler,
  private val languageService: LanguageService,
  private val securityService: SecurityService,
  private val authenticationFacade: AuthenticationFacade,
  private val screenshotService: ScreenshotService
) : IController {
  @GetMapping(value = ["/{languages}"])
  @AccessWithAnyProjectPermission
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
  @Operation(
    summary = "Returns all translations for specified languages",
    responses = [
      ApiResponse(
        responseCode = "200",
        content = [
          Content(
            schema = Schema(
              example = """{"en": {"what a key": "Translated value", "another key": "Another key translated"},""" +
                """"cs": {"what a key": "Překlad", "another key": "Další překlad"}}"""
            )
          )
        ]
      )
    ]
  )
  fun getAllTranslations(@PathVariable("languages") languages: Set<String>): Map<String, Any> {
    return translationService.getTranslations(languages, projectHolder.project.id)
  }

  @PutMapping("")
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
  @AccessWithProjectPermission(permission = Permission.ProjectPermissionType.TRANSLATE)
  @Operation(summary = "Sets translations for existing key")
  fun setTranslations(@RequestBody @Valid dto: SetTranslationsWithKeyDto): SetTranslationsResponseModel {
    val key = keyService.findOptional(
      projectHolder.project.id,
      PathDTO.fromFullPath(dto.key)
    ).orElseThrow { NotFoundException() }

    val modifiedTranslations = translationService.setForKey(key, dto.translations)

    val translations = dto.languagesToReturn
      ?.let { languagesToReturn ->
        key.translations
          .filter { languagesToReturn.contains(it.language.tag) }
          .associateBy { it.language.tag }
      }
      ?: modifiedTranslations

    return getSetTranslationsResponse(key, translations)
  }

  @PostMapping("")
  @AccessWithApiKey([ApiScope.TRANSLATIONS_EDIT])
  @AccessWithProjectPermission(permission = Permission.ProjectPermissionType.EDIT)
  @Operation(summary = "Sets translations for existing or not existing key")
  fun createOrUpdateTranslations(@RequestBody @Valid dto: SetTranslationsWithKeyDto): SetTranslationsResponseModel {
    checkScopesIfKeyExists(dto)
    val key = keyService.getOrCreateKey(projectHolder.projectEntity, PathDTO.fromFullPath(dto.key))
    val translations = translationService.setForKey(key, dto.translations)
    return getSetTranslationsResponse(key, translations)
  }

  @PutMapping("/{translationId}/set-state/{state}")
  @AccessWithApiKey([ApiScope.TRANSLATIONS_EDIT])
  @AccessWithProjectPermission(permission = Permission.ProjectPermissionType.TRANSLATE)
  @Operation(summary = "Sets translation state")
  fun setTranslationState(@PathVariable translationId: Long, @PathVariable state: TranslationState): TranslationModel {
    val translation = translationService.find(translationId) ?: throw NotFoundException()
    translation.checkFromProject()
    return translationModelAssembler.toModel(translationService.setState(translation, state))
  }

  @GetMapping(value = [""])
  @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.VIEW)
  @Operation(summary = "Returns translations in project")
  fun getTranslations(
    @ParameterObject params: GetTranslationsParams,
    @ParameterObject pageable: Pageable
  ): KeysWithTranslationsPageModel {
    val languages: Set<Language> = languageService
      .getLanguagesForTranslationsView(params.languages, projectHolder.project.id)

    val data = translationService.getViewData(projectHolder.project.id, pageable, params, languages)

    val keysWithScreenshots = getKeysWithScreenshots(data.map { it.keyId }.toList())

    if (keysWithScreenshots != null) {
      data.content.forEach { it.screenshots = keysWithScreenshots[it.keyId] ?: listOf() }
    }

    val cursor = if (data.content.isNotEmpty()) CursorUtil.getCursor(data.content.last(), data.sort) else null
    return pagedAssembler.toTranslationModel(data, languages, cursor)
  }

  @GetMapping(value = ["select-all"])
  @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.VIEW)
  @Operation(summary = "Get select all keys")
  fun getSelectAllKeyIds(
    @ParameterObject params: TranslationFilters,
    @ParameterObject pageable: Pageable
  ): SelectAllResponse {
    val languages: Set<Language> = languageService
      .getLanguagesForTranslationsView(params.languages, projectHolder.project.id)

    return SelectAllResponse(
      translationService.getSelectAllKeys(
        projectId = projectHolder.project.id,
        pageable = pageable,
        params = params,
        languages = languages
      )
    )
  }

  @PutMapping(value = ["/{translationId:[0-9]+}/dismiss-auto-translated-state"])
  @AccessWithApiKey([ApiScope.TRANSLATIONS_EDIT])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.TRANSLATE)
  @Operation(summary = """Removes "auto translated" indication""")
  fun dismissAutoTranslatedState(
    @PathVariable translationId: Long
  ): TranslationModel {
    val translation = translationService.get(translationId)
    translation.checkFromProject()
    translationService.dismissAutoTranslated(translation)
    return translationModelAssembler.toModel(translation)
  }

  private fun getKeysWithScreenshots(keyIds: Collection<Long>): Map<Long, MutableSet<Screenshot>>? {
    if (
      !authenticationFacade.isApiKeyAuthentication ||
      authenticationFacade.apiKey.scopesEnum.contains(ApiScope.SCREENSHOTS_VIEW)
    ) {
      return screenshotService.getKeysWithScreenshots(keyIds).map { it.id to it.screenshots }.toMap()
    }
    return null
  }

  private fun getSetTranslationsResponse(key: Key, translations: Map<String, Translation>):
    SetTranslationsResponseModel {
    return SetTranslationsResponseModel(
      keyId = key.id,
      keyName = key.name,
      translations = translations.entries.associate { (languageTag, translation) ->
        languageTag to translationModelAssembler.toModel(translation)
      }
    )
  }

  private fun checkScopesIfKeyExists(dto: SetTranslationsWithKeyDto) {
    keyService.findOptional(projectHolder.projectEntity.id, dto.key).orElse(null) ?: let {
      if (authenticationFacade.isApiKeyAuthentication) {
        securityService.checkApiKeyScopes(setOf(ApiScope.KEYS_EDIT), authenticationFacade.apiKey)
      }
    }
  }

  private fun Translation.checkFromProject() {
    if (this.key.project.id != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.TRANSLATION_NOT_FROM_PROJECT)
    }
  }
}
