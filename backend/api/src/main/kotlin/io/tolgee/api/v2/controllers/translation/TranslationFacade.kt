package io.tolgee.api.v2.controllers.translation

import io.tolgee.api.mcp.McpFacade
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.security.ProjectHolder
import io.tolgee.service.language.LanguageService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import org.springframework.stereotype.Service

@Service
class TranslationFacade(
  private val projectHolder: ProjectHolder,
  private val languageService: LanguageService,
  private val securityService: SecurityService,
  private val translationService: TranslationService,
) : McpFacade {
  /** Returns the key's translations in the requested (or all) languages the caller may view. */
  fun getKeyTranslations(
    key: Key,
    languages: Set<String>?,
  ): List<Translation> {
    val requestedTags =
      languages?.takeIf { it.isNotEmpty() }
        ?: languageService.findAll(projectHolder.project.id).map { it.tag }.toSet()
    val permittedTags = securityService.filterViewPermissionByTag(projectHolder.project.id, requestedTags)
    if (permittedTags.isEmpty()) return emptyList()
    return translationService.findForKeyByLanguages(key, permittedTags)
  }
}
