package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.AutoTranslationService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/keys/{keyId:[0-9]+}/auto-translate",
    "/v2/projects/keys/{keyId:[0-9]+}/auto-translate",
  ],
)
@Tag(name = "Auto Translation")
@Suppress("MVCPathVariableInspection")
class AutoTranslationController(
  private val autoTranslationService: AutoTranslationService,
  private val keyService: KeyService,
  private val projectHolder: ProjectHolder,
  private val securityService: SecurityService,
) {
  @PutMapping("")
  @Operation(
    summary = "Auto translates keys",
    description = """Uses enabled auto-translation methods.
You need to set at least one of useMachineTranslation or useTranslationMemory to true.

This will replace the the existing translation with the result obtained from specified source!
    """,
  )
  @RequiresProjectPermissions([ Scope.TRANSLATIONS_EDIT ])
  @AllowApiAccess
  fun autoTranslate(
    @PathVariable keyId: Long,
    @Parameter(
      description = """Tags of languages to auto-translate. 
When no languages provided, it translates only untranslated languages.""",
    )
    @RequestParam
    languages: Set<String>?,
    @RequestParam useMachineTranslation: Boolean?,
    @RequestParam useTranslationMemory: Boolean?,
  ) {
    val key = keyService.get(keyId)
    val languagesToTranslate =
      securityService.filterViewPermissionByTag(
        projectHolder.project.id,
        languages ?: getAllLanguagesToTranslate(),
      )

    checkPermissions(key, languagesToTranslate)
    validateServices(useMachineTranslation, useTranslationMemory)

    if (languages?.contains(getBaseLanguageTag()) == true) {
      throw BadRequestException(Message.CANNOT_TRANSLATE_BASE_LANGUAGE)
    }

    autoTranslationService.autoTranslateSyncWithRetry(
      key = key,
      forcedLanguageTags = languages?.toList(),
      useTranslationMemory = useTranslationMemory ?: false,
      useMachineTranslation = useMachineTranslation ?: false,
      isBatch = false,
    )
  }

  private fun validateServices(
    useMachineTranslation: Boolean?,
    useTranslationMemory: Boolean?,
  ) {
    if (useMachineTranslation != true && useTranslationMemory != true) {
      throw BadRequestException(Message.NO_AUTO_TRANSLATION_METHOD)
    }
  }

  private fun checkPermissions(
    key: Key,
    languagesToTranslate: Set<String>,
  ) {
    keyService.checkInProject(key, projectHolder.project.id)
    securityService.checkLanguageTranslatePermissionsByTag(languagesToTranslate, projectHolder.project.id, key.id)
  }

  private fun getAllLanguagesToTranslate(): Set<String> {
    val baseLanguageTag = getBaseLanguageTag()
    return projectHolder.projectEntity.languages.map { it.tag }.filter {
      it != baseLanguageTag
    }.toSet()
  }

  private fun getBaseLanguageTag(): String {
    return projectHolder.projectEntity.baseLanguage?.tag ?: throw NotFoundException(
      Message.BASE_LANGUAGE_NOT_FOUND,
    )
  }
}
