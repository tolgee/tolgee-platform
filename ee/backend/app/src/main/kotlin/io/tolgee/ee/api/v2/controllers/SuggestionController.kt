package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.suggestion.SuggestionFilters
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.ee.service.TranslationSuggestionServiceEeImpl
import io.tolgee.hateoas.translations.TranslationSuggestionModel
import io.tolgee.hateoas.translations.TranslationSuggestionModelAssembler
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.project.ProjectService
import jakarta.validation.Valid
import jakarta.websocket.server.PathParam
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/language/{languageId:[0-9]+}/key/{keyId:[0-9]+}/suggestion",
    "/v2/projects/language/{languageId:[0-9]+}/key/{keyId:[0-9]+}/suggestion",
  ],
)
@Tag(name = "Translation suggestion")
class SuggestionController(
  private val translationSuggestionService: TranslationSuggestionServiceEeImpl,
  private val projectHolder: ProjectHolder,
  private val translationSuggestionModelAssembler: TranslationSuggestionModelAssembler,
  private val arrayResourcesAssembler: PagedResourcesAssembler<TranslationSuggestion>,
  private val projectService: ProjectService,
) {
  @PostMapping("")
  @Operation(summary = "Create translation suggestion")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.TRANSLATIONS_SUGGEST])
  fun createSuggestion(
    @RequestBody @Valid
    dto: CreateTranslationSuggestionRequest,
    @PathVariable languageId: Long,
    @PathVariable keyId: Long,
  ): TranslationSuggestionModel {
    val projectId = projectHolder.project.id
    val suggestion = translationSuggestionService.createSuggestion(
      projectId, languageId, keyId, dto)
    return translationSuggestionModelAssembler.toModel(suggestion)
  }

  @GetMapping("")
  @Operation(summary = "Get suggestions")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  fun getSuggestions(
    @ParameterObject
    pageable: Pageable,
    @ParameterObject
    filters: SuggestionFilters,
    @PathVariable languageId: Long,
    @PathVariable keyId: Long,
  ): PagedModel<TranslationSuggestionModel> {
    val projectId = projectHolder.project.id
    val suggestions = translationSuggestionService.getSuggestionsPaged(
      pageable, projectId, languageId, keyId, filters
    )
    return arrayResourcesAssembler.toModel(
      suggestions,
      translationSuggestionModelAssembler
    )
  }

  @PutMapping("/{suggestionId:[0-9]+}/decline")
  @Operation(summary = "Discard suggestion")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  fun declineSuggestion(
    @PathVariable languageId: Long,
    @PathVariable keyId: Long,
    @PathVariable suggestionId: Long,
  ): TranslationSuggestionModel {
    val projectId = projectHolder.project.id
    val suggestion = translationSuggestionService.declineSuggestion(projectId, keyId, suggestionId)
    return translationSuggestionModelAssembler.toModel(suggestion)
  }

  @PutMapping("/{suggestionId:[0-9]+}/accept")
  @Operation(summary = "Discard suggestion")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  fun acceptSuggestion(
    @PathVariable languageId: Long,
    @PathVariable keyId: Long,
    @PathVariable suggestionId: Long,
  ): TranslationSuggestionModel {
    val projectId = projectHolder.project.id
    val suggestion = translationSuggestionService.acceptSuggestion(projectId, keyId, suggestionId)
    return translationSuggestionModelAssembler.toModel(suggestion)
  }
}
