package io.tolgee.api.v2.controllers.suggestion

import io.tolgee.api.mcp.McpFacade
import io.tolgee.constants.Feature
import io.tolgee.dtos.request.suggestion.SuggestionFilters
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.exceptions.NotImplementedInOss
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.service.translation.TranslationSuggestionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class SuggestionsFacade(
  private val translationSuggestionService: TranslationSuggestionService,
  private val translationService: TranslationService,
  private val languageService: LanguageService,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val projectFeatureGuard: ProjectFeatureGuard,
) : McpFacade {
  /** Returns null when the language does not exist or the caller may not view it. */
  fun getKeysWithSuggestions(
    languageTag: String,
    branch: String?,
    pageable: Pageable,
  ): Page<KeyWithTranslationsView>? {
    projectFeatureGuard.checkIfUsed(Feature.BRANCHING, branch)
    val projectId = projectHolder.project.id
    val languages =
      languageService.getLanguagesForTranslationsView(
        setOf(languageTag),
        projectId,
        authenticationFacade.authenticatedUser.id,
      )
    if (languages.isEmpty()) return null
    val params =
      GetTranslationsParams().apply {
        this.languages = setOf(languageTag)
        filterHasSuggestionsInLang = listOf(languageTag)
        this.branch = branch
      }
    return translationService.getViewData(projectId, pageable, params, languages)
  }

  fun getSuggestions(
    languageId: Long,
    keyId: Long,
    filters: SuggestionFilters,
    pageable: Pageable,
  ): Page<TranslationSuggestion> {
    val projectId = projectHolder.project.id
    securityService.checkLanguageViewSuggestionsPermission(projectId, listOf(languageId))
    return translationSuggestionService.getSuggestionsPaged(pageable, projectId, languageId, keyId, filters)
  }

  fun acceptSuggestion(
    languageId: Long,
    keyId: Long,
    suggestionId: Long,
    declineOther: Boolean,
  ): Pair<TranslationSuggestion, List<Long>> {
    val projectId = projectHolder.project.id
    securityService.checkLanguageTranslatePermission(projectId, listOf(languageId))
    securityService.checkLanguageStateChangePermission(projectId, listOf(languageId))
    return translationSuggestionService.acceptSuggestion(projectId, languageId, keyId, suggestionId, declineOther)
      ?: throw NotImplementedInOss()
  }

  fun declineSuggestion(
    languageId: Long,
    keyId: Long,
    suggestionId: Long,
  ): TranslationSuggestion {
    val projectId = projectHolder.project.id
    securityService.checkLanguageStateChangePermission(projectId, listOf(languageId))
    return translationSuggestionService.declineSuggestion(projectId, languageId, keyId, suggestionId)
      ?: throw NotImplementedInOss()
  }
}
