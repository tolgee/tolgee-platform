package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.request.suggestion.SuggestionFilters
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.ee.service.TranslationSuggestionServiceEeImpl
import io.tolgee.hateoas.translations.suggestions.TranslationSuggestionAcceptResponse
import io.tolgee.hateoas.translations.suggestions.TranslationSuggestionModel
import io.tolgee.hateoas.translations.suggestions.TranslationSuggestionModelAssembler
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.security.SecurityService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/languages/{languageId:[0-9]+}/key/{keyId:[0-9]+}/suggestion",
    "/v2/projects/languages/{languageId:[0-9]+}/key/{keyId:[0-9]+}/suggestion",
  ],
)
@Tag(name = "Suggestions")
class SuggestionController(
  private val translationSuggestionService: TranslationSuggestionServiceEeImpl,
  private val projectHolder: ProjectHolder,
  private val translationSuggestionModelAssembler: TranslationSuggestionModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val arrayResourcesAssembler: PagedResourcesAssembler<TranslationSuggestion>,
  private val authenticationFacade: AuthenticationFacade,
  private val securityService: SecurityService,
) {
  @GetMapping("")
  @Operation(summary = "Get suggestions")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @OpenApiOrderExtension(2)
  fun getSuggestions(
    @ParameterObject
    pageable: Pageable,
    @ParameterObject
    filters: SuggestionFilters,
    @PathVariable languageId: Long,
    @PathVariable keyId: Long,
  ): PagedModel<TranslationSuggestionModel> {
    securityService.checkLanguageSuggestPermission(
      projectHolder.project.id,
      listOf(languageId),
    )
    val projectId = projectHolder.project.id
    val suggestions =
      translationSuggestionService.getSuggestionsPaged(
        pageable,
        projectId,
        languageId,
        keyId,
        filters,
      )
    return arrayResourcesAssembler.toModel(
      suggestions,
      translationSuggestionModelAssembler,
    )
  }

  @PostMapping("")
  @Operation(summary = "Create translation suggestion")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.TRANSLATIONS_SUGGEST])
  @RequestActivity(ActivityType.CREATE_SUGGESTION)
  @OpenApiOrderExtension(3)
  fun createSuggestion(
    @RequestBody @Valid
    dto: CreateTranslationSuggestionRequest,
    @PathVariable languageId: Long,
    @PathVariable keyId: Long,
  ): TranslationSuggestionModel {
    val project = projectHolder.projectEntity
    securityService.checkLanguageSuggestPermission(
      project.id,
      listOf(languageId),
    )
    val suggestion =
      translationSuggestionService.createSuggestion(
        project,
        languageId,
        keyId,
        dto,
      )
    return translationSuggestionModelAssembler.toModel(suggestion)
  }

  @DeleteMapping("/{suggestionId:[0-9]+}")
  @Operation(
    summary = "Delete suggestion",
    description = "User can only delete suggestion created by them",
  )
  @AllowApiAccess
  // user can only delete suggestion created by them; it's checked in the service
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @RequestActivity(ActivityType.DELETE_SUGGESTION)
  @OpenApiOrderExtension(4)
  fun deleteSuggestion(
    @PathVariable languageId: Long,
    @PathVariable keyId: Long,
    @PathVariable suggestionId: Long,
  ) {
    val projectId = projectHolder.project.id
    translationSuggestionService.deleteSuggestionCreatedByUser(
      projectId,
      keyId,
      suggestionId,
      authenticationFacade.authenticatedUser.id,
    )
  }

  @PutMapping("/{suggestionId:[0-9]+}/decline")
  @Operation(summary = "Decline suggestion")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.TRANSLATIONS_STATE_EDIT])
  @RequestActivity(ActivityType.DECLINE_SUGGESTION)
  fun declineSuggestion(
    @PathVariable languageId: Long,
    @PathVariable keyId: Long,
    @PathVariable suggestionId: Long,
  ): TranslationSuggestionModel {
    securityService.checkLanguageStateChangePermission(
      projectHolder.project.id,
      listOf(languageId),
    )
    val projectId = projectHolder.project.id
    val suggestion = translationSuggestionService.declineSuggestion(projectId, keyId, suggestionId)
    return translationSuggestionModelAssembler.toModel(suggestion)
  }

  @PutMapping("/{suggestionId:[0-9]+}/accept")
  @Operation(summary = "Accept suggestion")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @RequestActivity(ActivityType.ACCEPT_SUGGESTION)
  fun acceptSuggestion(
    @PathVariable languageId: Long,
    @PathVariable keyId: Long,
    @PathVariable suggestionId: Long,
    @RequestParam declineOther: Boolean = false,
  ): TranslationSuggestionAcceptResponse {
    securityService.checkLanguageTranslatePermission(
      projectHolder.project.id,
      listOf(languageId),
    )
    securityService.checkLanguageStateChangePermission(
      projectHolder.project.id,
      listOf(languageId),
    )
    val projectId = projectHolder.project.id
    val (suggestion, declined) =
      translationSuggestionService.acceptSuggestion(
        projectId,
        languageId,
        keyId,
        suggestionId,
        declineOther,
      )
    val accepted = translationSuggestionModelAssembler.toModel(suggestion)
    return TranslationSuggestionAcceptResponse(accepted, declined)
  }

  @PutMapping("/{suggestionId:[0-9]+}/set-active")
  @Operation(summary = "Set suggestion active")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.TRANSLATIONS_STATE_EDIT])
  @RequestActivity(ActivityType.SUGGESTION_SET_ACTIVE)
  fun suggestionSetActive(
    @PathVariable languageId: Long,
    @PathVariable keyId: Long,
    @PathVariable suggestionId: Long,
  ): TranslationSuggestionModel {
    securityService.checkLanguageStateChangePermission(
      projectHolder.project.id,
      listOf(languageId),
    )
    val projectId = projectHolder.project.id
    val suggestion = translationSuggestionService.suggestionSetActive(projectId, keyId, suggestionId)
    return translationSuggestionModelAssembler.toModel(suggestion)
  }
}
