package io.tolgee.api.v2.controllers

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.key.Key
import io.tolgee.security.ProjectHolder
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.AutoTranslationService
import org.springframework.stereotype.Service

@Service
class AutoTranslationFacade(
  private val autoTranslationService: AutoTranslationService,
  private val keyService: KeyService,
  private val projectHolder: ProjectHolder,
  private val securityService: SecurityService,
) {
  fun autoTranslate(
    keyId: Long,
    languages: Set<String>?,
    useMachineTranslation: Boolean?,
    useTranslationMemory: Boolean?,
  ) {
    val key = keyService.get(keyId)
    keyService.checkInProject(key, projectHolder.project.id)
    val languagesToTranslate = getPermittedLanguagesToTranslate(languages, key)
    validateServices(useMachineTranslation, useTranslationMemory)

    if (languages?.contains(getBaseLanguageTag()) == true) {
      throw BadRequestException(Message.CANNOT_TRANSLATE_BASE_LANGUAGE)
    }

    autoTranslationService.autoTranslateSyncWithRetry(
      key = key,
      forcedLanguageTags = languagesToTranslate.toList(),
      overrideExisting = languages != null,
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

  private fun getPermittedLanguagesToTranslate(
    languages: Set<String>?,
    key: Key,
  ): Set<String> {
    val projectId = projectHolder.project.id
    if (languages != null) {
      securityService.checkLanguageTranslatePermissionsByTag(languages, projectId, key.id)
      return languages
    }
    val baseLanguageTag = getBaseLanguageTag()
    val candidates = projectHolder.projectEntity.languages.filter { it.tag != baseLanguageTag }
    val candidateIds = candidates.map { it.id }
    val permittedIds = securityService.filterTranslatePermissionByLanguageId(projectId, candidateIds)
    if (permittedIds.isEmpty()) {
      // Nothing permitted directly: the full check either throws the usual 403 or passes for a task assignee.
      securityService.checkLanguageTranslatePermissionsByLanguageId(candidateIds, projectId, key.id)
      return candidates.map { it.tag }.toSet()
    }
    return candidates.filter { it.id in permittedIds }.map { it.tag }.toSet()
  }

  private fun getBaseLanguageTag(): String {
    return projectHolder.projectEntity.baseLanguage?.tag ?: throw NotFoundException(
      Message.BASE_LANGUAGE_NOT_FOUND,
    )
  }
}
